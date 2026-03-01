Wacky Decoder submission

This project was created for the hackclub wacky data storage project

Explanation:

Minecraft uses a special type of data file, .mca to store chunks. Whever you load a new area or change some blocks, the .mca file for that region changes. I used this to create a system that should manipulate blockstates in order to store unique data, but it doesnt change anything, but since it still somehow stores data even though I don't know how (life of a programmer), I'm keeping it like this. If I ever come back to this, I might make it work how I thought it did, and give the ability to actually see your data as minecraft blocks, which would be pretty cool, along with being able to transfer files by just copying a build.

USAGE:
Since this is a file based technical project, I decided to leave it as open source and not create a compiled version because I'm lazy. Download the project and open in your IDE. in your IDE terminal, run "start run.bat". This will compile and run the program, but make sure you have a file input.* in the src\main\java beforehand. Choose one to encode the input into a minecraft region file located in target\classes, and 2 to decode it once you want to, or if another person wants to. Pressing one will give you an r.0.0.mca and pressing 2 will reconstruct input.*.