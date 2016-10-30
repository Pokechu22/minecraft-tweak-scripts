from jawa.cf import ClassFile
from zipfile import ZipFile
import re
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO
from jawa.assemble import assemble
import tempfile
import zipfile
import shutil
import os

jar_name = raw_input("Enter path and file name of server JAR: ")

with ZipFile(jar_name, "r") as jar:
    # Find the World class.  Can't hardcode it since it changes each version.
    pattern = re.compile(r'^net.minecraft.server.(.+?).World.class$')
    matches = [name for name in jar.namelist() if pattern.match(name)]
    print "World class: ", matches
    assert len(matches) == 1

    world_class_name = matches[0]

    world_class = ClassFile(StringIO(jar.read(world_class_name)))

get_height_method = None

methods = world_class.methods.find(args='', returns='I')
for method in methods:
    if not method or not method.code:
        # Abstract methods
        continue
    # Ugly part - find the right method
    # The method always has 'sipush' with 128 and 'sipush' with 256 in it
    # for the check.  So we can test for that.
    found_128 = False
    found_256 = False
    for ins in method.code.disassemble():
        if ins.mnemonic == "sipush":
            if ins.operands[0].value == 128:
                found_128 = True
            if ins.operands[0].value == 256:
                found_256 = True

    if found_128 and found_256:
        get_height_method = method
        break

if not get_height_method:
    print "Failed to find getActualHeight()."
    raise Exception()

print "getActualHeight():", get_height_method.name.value, get_height_method.descriptor.value
print
print "Old bytecode: "

for ins in get_height_method.code.disassemble():
    print ins

# Now, we write new code... in java bytecode.  Fun part.
# Fortunately we only need to do something very simplistic...
get_height_method.code.assemble(assemble([ 
    ('sipush', 256),
    ('return',) 
]))

print
print "New bytecode:"

for ins in get_height_method.code.disassemble():
    print ins

print

# Remove the old world class (zip files, being strange, allow multiple with the same name)
# http://stackoverflow.com/a/4653863/3991344 - nosklo
def remove_from_zip(zipfname, *filenames):
    tempdir = tempfile.mkdtemp()
    try:
        tempname = os.path.join(tempdir, 'new.zip')
        with zipfile.ZipFile(zipfname, 'r') as zipread:
            with zipfile.ZipFile(tempname, 'w') as zipwrite:
                for item in zipread.infolist():
                    if item.filename not in filenames:
                        data = zipread.read(item.filename)
                        zipwrite.writestr(item, data)
        shutil.move(tempname, zipfname)
    finally:
        shutil.rmtree(tempdir)
# End stackoverflow copypaste

print "Removing old world class from zipfile..."
remove_from_zip(jar_name, world_class_name)

print "Writing new world class..."
# Now we need to save:
out = StringIO()
world_class.save(out)

with ZipFile(jar_name, "a") as jar:
    jar.writestr(world_class_name, out.getvalue())

print "Saved!  Your server jar should now be updated."