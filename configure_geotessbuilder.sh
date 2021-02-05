#!/bin/bash

# This script creates bash script in the current directory
# called 'geotessbuilder'.  It will call the geotessbuilder.jar 
# file as a runnable jar.
# 
# The script also prints to screen recommended addition to 
# the user's .cshrc or .bash_profile that will make the new
# executable available via the PATH.  No changes to the user's
# environment are actually made.

echo "Creating executable script file geotessbuilder that launches GeoTessBuilder"
echo "#!/bin/bash" > geotessbuilder
echo "#" >> geotessbuilder
echo "# The substring '-Xmx????m' in the following execution" >> geotessbuilder
echo "# command specifies the amount of memory to make available" >> geotessbuilder
echo "# to the application, in megabytes." >> geotessbuilder
echo "#" >> geotessbuilder
echo "java -Xmx1400m -jar `pwd`/geotessbuilder.jar \$*" >> geotessbuilder
chmod 777 geotessbuilder
echo "Recommended modification to environment:"

if [ `uname -s` = Darwin ]; then
	echo "export PATH=`pwd`:\$PATH"
else
	echo "set path=( `pwd` \$path )"
fi
