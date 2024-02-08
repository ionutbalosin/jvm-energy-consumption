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

local math = require("math")

local threads = {}
local requests = {}
local petTypes = { "bird", "cat", "dog", "hamster", "lizard", "snake" }
local dates = { "2001-01-01", "2002-02-02", "2003-03-03", "2004-04-04", "2005-05-05", "2006-06-06", "2007-07-07", "2008-08-08", "2009-09-09", "2010-10-10", "2010-11-11", "2010-12-12" }
local headers = {["Content-Type"] = "application/x-www-form-urlencoded"}

function setup(thread)
    thread:set("owner_url", nil)
    thread:set("pet_url", nil)
    thread:set("pet_visit_url", nil)
    thread:set("current_request_index", 1)

    table.insert(threads, thread)
end

function random_number()
    return math.random(100000000, 999999999)
end

function request_create_owner()
    local path = "/owners/new"
    local randomNumber = random_number()
    local randomDate = dates[math.random(1, #dates)]
    local randomPetType = petTypes[math.random(1, #petTypes)]
    local owner = "firstName=Owner" ..
      "&lastName=" .. randomNumber ..
      "&address=Address-" .. randomNumber ..
      "&city=City-" .. randomNumber ..
      "&telephone=" .. randomNumber

    return wrk.format("POST", path, headers, owner)
end

function request_get_owner()
    local owner_url = wrk.thread:get("owner_url")
    if not owner_url then
        return nil -- no further requests to execute
    end

   return wrk.format("GET", owner_url, headers)
end

function request_create_pet()
    local owner_url = wrk.thread:get("owner_url")
    if not owner_url then
        return nil -- no further requests to execute
    end

    local path = owner_url .. "/pets/new"
    local randomNumber = random_number()
    local randomDate = dates[math.random(1, #dates)]
    local randomPetType = petTypes[math.random(1, #petTypes)]
    local pet = "name=Pet-" .. randomNumber ..
        "&birthDate=" .. randomDate ..
        "&type=" .. randomPetType

   return wrk.format("POST", path, headers, pet)
end

function request_get_pet()
    local pet_url = wrk.thread:get("pet_url")
    if not pet_url then
        return nil -- no further requests to execute
    end

    local path = "/owners/" .. pet_url
    return wrk.format("GET", path, headers)
end

function request_create_visit()
    local pet_visit_url = wrk.thread:get("pet_visit_url")
    if not pet_visit_url then
        return nil -- no further requests to execute
    end

    local path = "/owners/" .. pet_visit_url
    local randomNumber = random_number()
    local randomDate = dates[math.random(1, #dates)]
    local pet = "description=Visit-" .. randomNumber ..
        "&date=" .. randomDate

   return wrk.format("POST", path, headers, pet)
end

function init(args)
    requests[1] = request_create_owner
    requests[2] = request_create_pet
    requests[3] = request_get_owner
    requests[4] = request_create_visit

    return requests
end

function request()
    local request_index = wrk.thread:get("current_request_index")
    local request = requests[request_index]()
    if not request then
        return nil -- no further requests to execute
    end

    request_index = (request_index % #requests) + 1
    wrk.thread:set("current_request_index", request_index)

    return request
end

function response(status, headers, body)
    if status >= 200 and status < 300 then
        -- attempt to read the add pet visits URL from the HTML response; not available otherwise (due to server limitation)
        local add_pet_visit_url_pattern = '<td><a href="([^"]+/pets/%d+/visits/new)">Add Visit</a></td>'
        local is_add_pet_visit_url = body:match(add_pet_visit_url_pattern)
        if is_add_pet_visit_url then
            wrk.thread:set("pet_visit_url", is_add_pet_visit_url)
        end

        -- attempt to read the edit pet URL from the HTML response; not available otherwise (due to server limitation)
        local edit_pet_url_pattern = '<td><a href="([^"]+/pets/%d+/edit)">Edit Pet</a></td>'
        local is_edit_pet_url = body:match(edit_pet_url_pattern)
        if is_edit_pet_url then
            wrk.thread:set("pet_url", is_edit_pet_url)
        end
    end

    -- in general, the redirection is back to the '/owners/{ownerId}' home page.
    if status >= 300 and status < 400 then
        local redirect_url = headers["Location"]
        local is_owners_url = string.match(redirect_url, "/owners/(%d+)$")
        if is_owners_url then
            wrk.thread:set("owner_url", redirect_url)
        end
    end

    if status >= 400 then
        print("ERROR: status code: ", status)
    end
end

done = function(summary, latency, requests)
   io.write("------------------------------\n")
   io.write("Summary:\n")
   io.write(string.format("Total requests: %d\n", summary.requests))
   io.write(string.format("Total socket connection errors: %d\n", summary.errors.connect))
   io.write(string.format("Total socket read errors: %d\n", summary.errors.read))
   io.write(string.format("Total socket write errors: %d\n", summary.errors.write))
   io.write(string.format("Total errors status: %d\n", summary.errors.status))
   io.write(string.format("Total errors timeouts: %d\n", summary.errors.timeout))
   io.write("------------------------------\n")
   io.write("Statistics:\n")
   for _, p in pairs({ 50, 75, 90, 94, 98, 99, 99.9, 99.99, 99.999, 99.9999 }) do
      n = latency:percentile(p)
      io.write(string.format("%g%%, %d ms\n", p, n / 1000))
   end
end