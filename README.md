# Goobi workflow Plugin: goobi-plugin-step-vocabulary-enrichment

<img src="https://goobi.io/wp-content/uploads/logo_goobi_plugin.png" align="right" style="margin:0 0 20px 20px;" alt="Plugin for Goobi workflow" width="175" height="109">

This Step plugin for Goobi workflow was developed for the yerusha project. It allows a configuration to define a metadata field (using the Goobi internal Variable-Replacer) to support reading Metadata from the METS-file or from process properties (e.g. for original access locations). It then checks if the metadata value from the configured field exists as main entry or inside of a configurable field inside of the defined vocabulary already. If the value from the configured field is not in the vocabulary already, it creates a new vocabulary record and writes the value into the configured field. Additionally it writes configured information into other fields of the same vocabulary record.

This is a plugin for Goobi workflow, the open source workflow tracking software for digitisation projects. More information about Goobi workflow is available under https://goobi.io. If you want to get in touch with the user community simply go to https://community.goobi.io.

## Plugin details

More information about the functionality of this plugin and the complete documentation can be found in the central documentation area at https://docs.goobi.io

Detail                      | Description
--------------------------- | ----------------------
**Plugin identifier**       | intranda_step_vocabulary_enrichment
**Plugin type**             | step
**Licence**                 | GPL 2.0 or newer
**Documentation (German)**  | https://docs.goobi.io/workflow-plugins/v/ger/step/goobi-plugin-step-vocabulary-enrichment
**Documentation (English)** | https://docs.goobi.io/workflow-plugins/v/eng/step/goobi-plugin-step-vocabulary-enrichment

## Goobi details

Goobi workflow is an open source web application to manage small and large digitisation projects mostly in cultural heritage institutions all around the world. More information about Goobi can be found here:

Detail                      | Description
--------------------------- | ---------------------------
**Goobi web site**          | https://www.goobi.io
**Goobi community**         | https://community.goobi.io
**Goobi documentation**     | https://docs.goobi.io

## Development

This plugin was developed by intranda. If you have any issues, feedback, question or if you are looking for more information about Goobi workflow, Goobi viewer and all our other developments that are used in digitisation projects please get in touch with us.  

Contact                     | Details
--------------------------- | ----------------------------------------------------
**Company name**            | intranda GmbH
**Address**                 | Bertha-von-Suttner-Str. 9, 37085 Göttingen, Germany
**Web site**                | https://www.intranda.com
