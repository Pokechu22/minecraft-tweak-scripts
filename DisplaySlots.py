"""
Makes Slot.slotIndex public and then appends this code to the end of GuiContainer.drawSlot(Slot s):

Minecraft.getMinecraft().fontRendererObj.drawString(Integer.toString(s.slotIndex), s.xDisplayPosition + 4, s.yDisplayPosition + 3, 0);

This makes slots render their IDs.
"""

from jawa.cf import ClassFile
from jawa.constants import *
from jawa.util.bytecode import Instruction
from jawa.assemble import assemble
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

brand_class = None
container_class = None
guicontainer_class = None
fontrenderer_class = None
minecraft_class = None
slot_class = None

remove_names = []

with ZipFile(jar_name, "r") as jar:
    print "Searching for classes..."

    for path in jar.namelist():
        if path.startswith("META-INF"):
            remove_names.append(path)
        if not path.endswith(".class"):
            continue

        cf = ClassFile(StringIO(jar.read(path)))

        if cf.this.name.value == "net/minecraft/client/ClientBrandRetriever":
            brand_class = cf
            remove_names.append(path)

        for c in cf.constants.find(ConstantString):
            if c.string.value == "Listener already listening":
                print "Container = %s" % cf.this.name.value
                container_class = cf
                # Continue searching classes, stop searching constants
                break
            elif c.string.value == "textures/gui/container/inventory.png":
                print "GuiContainer = %s" % cf.this.name.value
                guicontainer_class = cf
                remove_names.append(path)
                break
            elif c.string.value == "textures/font/unicode_page_%02x.png":
                print "FontRenderer = %s" % cf.this.name.value
                fontrenderer_class = cf
                break
            elif c.string.value == "textures/font/ascii.png":
                print "Minecraft = %s" % cf.this.name.value
                minecraft_class = cf
                break

    slot_class = None

    for method in container_class.methods:
        if len(method.args) == 1 and method.access_flags.acc_protected:
            path = method.args[0].name + ".class"
            slot_class = ClassFile(StringIO(jar.read(path)))
            print "Slot = %s" % slot_class.this.name.value
            remove_names.append(path)
            break

fontrendererobj = minecraft_class.fields.find_one(type_="L" + fontrenderer_class.this.name.value + ";")
print "Minecraft.fontRendererObj = %s" % (fontrendererobj.descriptor.value + " " + fontrendererobj.name.value)
draw_string = fontrenderer_class.methods.find_one(args="Ljava/lang/String;FFI")
print "FontRenderer.drawStringWithShadow = %s" % (draw_string.descriptor.value + " " + draw_string.name.value)
slot_fields = list(slot_class.fields)
slot_id = slot_fields[0]
slot_x = slot_fields[3]
slot_y = slot_fields[4]
print "Slot.slotIndex = %s, Slot.x = %s, Slot.y = %s" % (slot_id.descriptor.value + " " + slot_id.name.value, slot_x.descriptor.value + " " + slot_x.name.value, slot_y.descriptor.value + " " + slot_y.name.value)
draw_slot = guicontainer_class.methods.find_one(args="L" + slot_class.this.name.value + ";", f=lambda m: m.access_flags.acc_private)
print "GuiContainer.drawSlot = %s" % (draw_slot.descriptor.value + " " + draw_slot.name.value)
get_minecraft = minecraft_class.methods.find_one(returns="L" + minecraft_class.this.name.value + ";")
print "Minecraft.getMinecraft = %s" % (get_minecraft.descriptor.value + " " + get_minecraft.name.value)

print "Generating new code..."

def new_code():
    old_instructions = list(draw_slot.code.disassemble())  # All instructions but the return
    """Generates new code for the draw slot method"""
    new_instructions = assemble([
        # Get the font renderer
        ('invokestatic', # Invoke Minecraft.getMinecraft()
            guicontainer_class.constants.create_method_ref(
                minecraft_class.this.name.value,  # Class
                get_minecraft.name.value,  # getMinecraft() name
                get_minecraft.descriptor.value  # getMinecraft() descriptor
            )
        ),
        ('getfield',  # Get the font renderer obj field
            guicontainer_class.constants.create_field_ref(
                minecraft_class.this.name.value,  # Class
                fontrendererobj.name.value,  # fontrendererobj field name
                fontrendererobj.descriptor.value  # fontrendererobj field descriptor
            )
        ),

        # Prepare the arguments
        ('aload_1',),  # Load the slot parameter
        ('getfield',  # Get the slot type field
            guicontainer_class.constants.create_field_ref(
                slot_class.this.name.value,  # Slot class
                slot_id.name.value,  # Slot ID field name
                slot_id.descriptor.value  # Slot ID field type
            )
        ),
        ('invokestatic', # Invoke Integer.toString(int value)
            guicontainer_class.constants.create_method_ref(
                'java/lang/Integer',  # Class
                'toString',  # Method named toString
                '(I)Ljava/lang/String;'  # Takes an int and returns a String
            )
        ),

        ('aload_1',),  # Load the slot parameter
        ('getfield',  # Get the slot type field
            guicontainer_class.constants.create_field_ref(
                slot_class.this.name.value,  # Slot class
                slot_x.name.value,  # Slot x field name
                slot_x.descriptor.value  # Slot x field type
            )
        ),
        ('iconst_3',),  # Put 3 on the stack
        ('iadd',), # Add 3 to x
        ('i2f',),  # Make it into a float

        ('aload_1',),  # Load the slot parameter
        ('getfield',  # Get the slot type field
            guicontainer_class.constants.create_field_ref(
                slot_class.this.name.value,  # Slot class
                slot_y.name.value,  # Slot y field name
                slot_y.descriptor.value  # Slot y field type
            )
        ),
        ('iconst_4',),  # Put 4 on the stack
        ('iadd',),  # Add 4 to y
        ('i2f',),  # Make it into a float

        ('ldc_w', guicontainer_class.constants.create_integer(0xFFFFFF)),  # Put the color 0xFFFFFF onto the stack (so, gray / &f)
        # drawString does make this solid, IE 0xFFFFFFFF.

        # Call drawString
        ('invokevirtual',
            guicontainer_class.constants.create_method_ref(
                fontrenderer_class.this.name.value,  # Slot class
                draw_string.name.value,  # Draw string field name
                draw_string.descriptor.value  # Draw string field type
            )
        ),
        ('pop',)  # Remove the return value from drawString
    ])
    
    for ins in old_instructions[:-1]:  # Old code (except for last return)
        yield ins
    for ins in new_instructions:  # New code
        yield ins
    yield old_instructions[-1]  # Final return

for ins in new_code():
    print ins

    
# Change the code for the draw slot method
draw_slot.code.assemble(new_code())
# Make slot ID public
slot_id.access_flags.acc_public = True
slot_id.access_flags.acc_private = False
# Tweak the brand
brand_class.constants.find_one(ConstantString).value = "vanilla + SlotIDDisplay"

# OK, now we've made the changes - save them!

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
    for c in (guicontainer_class, slot_class, brand_class):
        print "Writing " + c.this.name.value
        out = StringIO()
        c.save(out)
        jar.writestr(c.this.name.value + ".class", out.getvalue())

print "Saved!  Your jar should now be updated."