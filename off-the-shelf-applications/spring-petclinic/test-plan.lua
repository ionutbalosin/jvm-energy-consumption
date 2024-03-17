--
-- JVM Energy Consumption
--
-- MIT License
--
-- Copyright (c) 2023-2024 Ionut Balosin, Ko Turk
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
-- SOFTWARE.
--

-- Note: When launched with multiple concurrent threads, there is no guarantee
-- that the object created within the request() function is handled within
-- the response() function by the same thread.
-- Therefore, any thread could read the response triggered by another thread.

local math = require("math")

local threads = {}
local requests = {}
local petTypes = { "bird", "cat", "dog", "hamster", "lizard", "snake" }
local dates = { "2001-01-01", "2002-02-02", "2003-03-03", "2004-04-04", "2005-05-05", "2006-06-06", "2007-07-07", "2008-08-08", "2009-09-09", "2010-10-10", "2010-11-11", "2010-12-12" }
local headers = {["Content-Type"] = "application/x-www-form-urlencoded"}
local ownerUrlPattern = '/owners/(.*)'
local addPetVisitUrlPattern = '<td><a href="([^"]+/pets/%d+/visits/new)">Add Visit</a></td>'

function setup(thread)
    thread:set("owner_url", nil)
    thread:set("add_pet_visit_url", nil)
    thread:set("request_index", 1)

    table.insert(threads, thread)
end

function getRandomNumber()
    return math.random(100000000, 999999999)
end

function requestCreateOwner()
    local path = "/owners/new"
    local randomNumber = getRandomNumber()
    local randomDate = dates[math.random(1, #dates)]
    local randomPetType = petTypes[math.random(1, #petTypes)]
    local owner = "firstName=Owner" ..
      "&lastName=" .. randomNumber ..
      "&address=Address-" .. randomNumber ..
      "&city=City-" .. randomNumber ..
      "&telephone=" .. randomNumber

    return wrk.format("POST", path, headers, owner)
end

function requestGetOwner()
    local path = wrk.thread:get("owner_url")
    if not path then
        return nil -- no further requests to execute
    end
   return wrk.format("GET", path, headers, nil)
end

function requestCreatePet()
    local owner_url = wrk.thread:get("owner_url")
    if not owner_url then
        return nil -- no further requests to execute
    end
    local path = owner_url .. "/pets/new"
    local randomNumber = getRandomNumber()
    local randomDate = dates[math.random(1, #dates)]
    local randomPetType = petTypes[math.random(1, #petTypes)]
    local pet = "name=Pet-" .. randomNumber ..
        "&birthDate=" .. randomDate ..
        "&type=" .. randomPetType
   return wrk.format("POST", path, headers, pet)
end

function requestCreatePetVisit()
    local add_pet_visit_url = wrk.thread:get("add_pet_visit_url")
    if not add_pet_visit_url then
        return nil -- no further requests to execute
    end

    local path = "/owners/" .. add_pet_visit_url
    local randomNumber = getRandomNumber()
    local randomDate = dates[math.random(1, #dates)]
    local pet_visit = "description=Visit-" .. randomNumber ..
        "&date=" .. randomDate

   return wrk.format("POST", path, headers, pet_visit)
end

function init(args)
    requests[1] = requestCreateOwner
    requests[2] = requestCreatePet
    -- The GET owner request retrieves the owner and the pet
    requests[3] = requestGetOwner
    requests[4] = requestCreatePetVisit
    -- The GET owner request retrieves the owner, the pet, and the visit
    requests[5] = requestGetOwner

    return requests
end

function request()
    local request_index = wrk.thread:get("request_index")
    local request = requests[request_index]()

    if request_index == 5 then -- 5 is the requests length
        request_index = 1
    else
        request_index = request_index + 1
    end
    wrk.thread:set("request_index", request_index)

    if not request then
        return nil -- no further requests to execute
    else
        return request
    end
end

function response(status, headers, body)
    if status >= 200 and status < 300 then
        -- attempt to read the add pet visits URL from the HTML response; not available otherwise (due to server limitation)
        local isAddPetVisitUrl = body:match(addPetVisitUrlPattern)
        if isAddPetVisitUrl then
            wrk.thread:set("add_pet_visit_url", isAddPetVisitUrl)
        end
    end

    -- in general, the redirection is back to the '/owners/{ownerId}' home page.
    if status >= 300 and status < 400 then
        local redirect_url = headers["Location"]
        local isOwnersUrlWithSessionId = string.match(redirect_url, ownerUrlPattern)
        local context_root = redirect_url:match("(https?://[^/]+)/([^/]+)")
        local ownerId = isOwnersUrlWithSessionId:gsub("(.*);.*", "%1")
        local ownerPath = context_root .. "/owners/" .. ownerId
        if ownerPath then
            wrk.thread:set("owner_url", ownerPath)
        end
    end
end

done = function(summary, latency, requests)
   io.write("------------------------------\n")
    io.write("Summary statistics:\n")
    io.write(string.format("  Total requests: %d\n", summary.requests))
    io.write(string.format("  Total socket connection errors: %d\n", summary.errors.connect))
    io.write(string.format("  Total socket read errors: %d\n", summary.errors.read))
    io.write(string.format("  Total socket write errors: %d\n", summary.errors.write))
    io.write(string.format("  Total HTTP errors (i.e., status codes > 399): %d\n", summary.errors.status))
    io.write(string.format("  Total timeout errors: %d\n", summary.errors.timeout))
   io.write("------------------------------\n")
   io.write("Latency percentiles:\n")
   local percentiles = {}
   for i = 1, 99 do
      table.insert(percentiles, i)
   end
   for _, p in ipairs({99.9, 99.99, 99.999, 99.9999, 100}) do
      table.insert(percentiles, p)
   end
   for _, p in ipairs(percentiles) do
      n = latency:percentile(p)
      io.write(string.format("%7g%% %8.2fms\n", p, n / 1000))
   end
  io.write("  Note: Please take these latency percentiles with caution since wrk suffers from the Coordinated Omission problem. To get accurate latency measurements, use wrk2.\n")
end