<?xml version="1.0" encoding="UTF-8"?>
<metadata-profile xmlns="http://nuix.com/fbi/metadata-profile">
  <metadata-list>
    <metadata type="SPECIAL" name="Army Tank">
      <scripted-expression>
        <type>ruby</type>
        <script><![CDATA[java_import "com.nuix.proserv.t3k.ws.MetadataProfileBase"
MetadataProfileBase::display_object_data $current_item.custom_metadata, "army_tank"]]></script>
      </scripted-expression>
    </metadata>

  </metadata-list>
</metadata-profile>