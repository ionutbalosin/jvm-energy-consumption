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
local headers = {["Content-Type"] = "application/json"}
local fruits = { "Apple", "Bananas", "Oranges", "Pineapple", "Grape", "Strawberry", "Watermelon", "Pears", "Cherries", "Peach", "Lemon", "Avocado", "Blueberries", "Raspberry", "Blackberry", "Plums", "Muskmelon", "Papaya", "Mangoes" }
local fruitIdPattern = '{"id":(%d+),"name":"([^"]+)"'

function setup(thread)
    thread:set("fruit_id", nil)
    thread:set("request_index", 1)

    table.insert(threads, thread)
end

function getRandomNumber()
    return math.random(1, 999999999)
end

function requestCreateFruit()
    local path = "/entity/fruits"
    local randomNumber = getRandomNumber()
    local randomFruit = fruits[math.random(1, #fruits)]
    local fruit = '{"name": "' .. randomFruit .. ' - ' .. randomNumber .. '"}'

    return wrk.format("POST", path, headers, fruit)
end

function requestGetFruit()
    local fruit_id = wrk.thread:get("fruit_id")
    if not fruit_id then
        return nil -- no further requests to execute
    end

   local path = "/entity/fruits/" .. fruit_id
   return wrk.format("GET", path, headers, nil)
end

function requestUpdateFruit()
    local fruit_id = wrk.thread:get("fruit_id")
    if not fruit_id then
        return nil -- no further requests to execute
    end

    local path = "/entity/fruits/" .. fruit_id
    local randomNumber = getRandomNumber()
    local randomFruit = fruits[math.random(1, #fruits)]
    local fruit = '{"name": "' .. randomFruit .. ' - ' .. randomNumber .. ' upd"}'
    return wrk.format("PUT", path, headers, fruit)
end

function requestDeleteFruit()
    local fruit_id = wrk.thread:get("fruit_id")
    if not fruit_id then
        return nil -- no further requests to execute
    end

   -- set fruit_id to nil to prevent duplicate deletions
   wrk.thread:set("fruit_id", isFruitId)

   local path = "/entity/fruits/" .. fruit_id
   return wrk.format("DELETE", path, headers, nil)
end

function init(args)
    requests[1] = requestCreateFruit
    requests[2] = requestGetFruit
    requests[3] = requestUpdateFruit
    requests[4] = requestDeleteFruit

    return requests
end

function request()
    local request_index = wrk.thread:get("request_index")
    local request = requests[request_index]()
    if not request then
        return nil -- no further requests to execute
    end

    request_index = (request_index % #requests) + 1
    wrk.thread:set("request_index", request_index)

    return request
end

function response(status, headers, body)
    -- attempt to read the fruit id from the HTML response
    local isFruitId = body:match(fruitIdPattern)
    if isFruitId then
        wrk.thread:set("fruit_id", isFruitId)
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