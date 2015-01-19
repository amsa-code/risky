file = file("target/123456790.track","rb")
readSingle = function() readBin(file, single(), size=4, endian="big")
readLong = function() readBin(file, integer(), size=8, endian="big")
readByte = function() readBin(file, integer(), size=1, endian="big")
readShort = function() readBin(file, integer(), size=2, endian="big") 
assertEquals = function(expected, actual) 
  if (expected != actual) { warning(sprintf("expected %s but was %s", expected, actual)) }

lat = readSingle()
lon = readSingle()
time = readLong()
nav = readByte()
rot = readByte()
sog = readShort()
cog = readShort()
heading = readShort()
cls = readByte()

print(lat)
print(lon)
print(time)
print(nav)
print(rot)
print(sog)
print(cog)
print(heading)
print(cls)

# test returned values
assertEquals(-10, lat)
assertEquals(135, lon)
assertEquals(1421708455237, time)
assertEquals(7, nav)
assertEquals(75, sog)
assertEquals(450, cog)
assertEquals(46, heading)
assertEquals(1, cls)


