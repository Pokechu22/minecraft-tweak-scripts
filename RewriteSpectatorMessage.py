"""
Makes spectatorCantUse message appear in chat rather than actionbar
"""

from jawa.cf import ClassFile
from jawa.constants import *
from jawa.util.bytecode import Instruction
from zipfile import ZipFile
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO
import tempfile
import zipfile
import shutil
import os

jar_name = raw_input("Enter path and file name of JAR: ")

classes = []
remove_names = []

with ZipFile(jar_name, "r") as jar:
    print "Searching for classes..."

    for path in jar.namelist():
        if path.startswith("META-INF"):
            remove_names.append(path)
        if not path.endswith(".class"):
            continue

        cf = ClassFile(StringIO(jar.read(path)))
        for c in cf.constants.find(ConstantString):
            if c.string.value == "container.spectatorCantOpen":
                print "Found %s" % cf.this.name.value
                classes.append(cf)
                remove_names.append(path)
                # Continue searching classes
                break

print "classes:", [cf.this.name.value for cf in classes]

for cf in classes:
    for method in cf.methods:
        if not method or not method.code:
            # Abstract methods
            continue

        rewrite_next_iconst = False
        changed_method = False
        i = 0
        instructions = list(method.code.disassemble())

        while i < len(instructions):
            ins = instructions[i]
            if ins.mnemonic in ("ldc", "ldc_w"):
                const = cf.constants.get(ins.operands[0].value)
                if isinstance(const, ConstantString):
                    if const.string.value == "container.spectatorCantOpen":
                        rewrite_next_iconst = True
            elif ins.mnemonic == "iconst_1":
                if rewrite_next_iconst:
                    instructions[i] = Instruction.from_mnemonic("iconst_0")
                    print "Rewrote %s to %s in %s (%s)" % (ins, instructions[i], method.name.value, method.descriptor.value)
                    changed_method = True
                    rewrite_next_iconst = False
            i = i + 1

        if changed_method:
            print "Updated %s (%s)" % (method.name.value, method.descriptor.value)
            print "Old bytecode:"
            for ins in method.code.disassemble():
                print ins

            method.code.assemble(instructions)

            print "New bytecode:"
            for ins in method.code.disassemble():
                print ins

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

print "Removing old classes and META-INF from zipfile..."
remove_from_zip(jar_name, *remove_names)

print "Writing new classes..."

with ZipFile(jar_name, "a") as jar:
    for c in classes:
        out = StringIO()
        c.save(out)
        jar.writestr(c.this.name.value + ".class", out.getvalue())

print "Saved!  Your jar should now be updated."
