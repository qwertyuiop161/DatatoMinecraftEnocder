Wacky Decoder submission

This project was created for the hackclub wacky data storage project

Explanation:

Minecraft uses a special type of data file, .mca to store chunks. Whever you load a new area or change some blocks, the .mca file for that region changes. I used this to create a system that should manipulate blockstates in order to store unique data, but it doesnt change anything, but since it still somehow stores data even though I don't know how (life of a programmer), I'm keeping it like this. If I ever come back to this, I might make it work how I thought it did, and give the ability to actually see your data as minecraft blocks, which would be pretty cool, along with being able to transfer files by just copying a build.

USAGE:
Download MCAConverter.java. Run it in a folder with only that jar and a file named either input.* or r.0.0.mca. it will output the other type either encoded or decoded, eg run it with input.png, it creates r.0.0.mca, run that same r.0.0.mca through alone, it will recreate input.png
