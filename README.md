# Assimagilator

A command-line utility that "fits" a source image onto one or more destination images, keeping the destination image size and format.

This is useful for generating icons for app stores that require specific formats.  You can create a set of generic images in the correct size and format, then run "fitimg" to generate your icon in the specific formats.

## Installation

### Using NPM

    npm install assimagilator
	
### Manually

Download  [Assimilator.jar](bin/Assimilator.jar)


## Usage

### If you installed using npm

    fitimg path/to/src/image.png path/to/dest/image.png path/to/dest/image2.png ... path/to/dest/imagen.png
	
This will "fit" the src/image.png onto the specified destination images, with a transparent background.  

WARNING: This will overwrite the destination images.

#### If you installed manually

    java -jar Assimilator.jar path/to/src/image.png path/to/dest/image.png path/to/dest/image2.png ... path/to/dest/imagen.png
	
### Using a directory for destination

You can specify a directory as the destination file, in which case, it will fit the source image on all images that it finds inside that directory.  It will recurse all directories within the destination directory too.  

NOTE: You need to include the "-r" flag to use a directory as a destination.

E.g.

    fitimg srcimg.png -r destDirectory
	
### Setting the background color

You can provide a background color using Hex RGB values.  E.g.

    fitimg "#FFFFFF" srcimg.png destimg.png
	
This would use white for the background color.

## Supported image formats

This uses Java ImageIO for reading and writing images, so most major formats are supported.  You can add more formats by adding the appropriate SPI jars to the classpath.
	