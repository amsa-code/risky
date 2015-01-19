file = file("target/123456790.track","rb")
vals = readBin(file, double(), size=8, endian="little")
print(vals)
