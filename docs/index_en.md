---
title: Vocabulary enrichment
identifier: intranda_step_vocabulary_enrichment
description: Step Plugin for the Automatic Enrichment of Vocabulary
published: true
---

## Introduction
This documentation provides a brief overview of this plugin, which is used to enrich vocabularies based on metadata found in METS-MODS files within Goobi workflows.

## Installation
To be able to use the plugin, the following files must be installed:

```bash
/opt/digiverso/goobi/plugins/step/plugin_intranda_step_vocabulary_enrichment.jar
/opt/digiverso/goobi/config/plugin_intranda_step_vocabulary_enrichment.xml
```

Once the plugin has been installed, it can be selected within the workflow for the respective work steps and thus executed automatically. 

## Overview and functionality
The program examines the metadata fields of a METS-MODS file from a Goobi process. For each `<item>` defined in the configuration file, the plugin will search the METS-MODS file for the specified metadata. The search can be restricted to the top struct of a document using the form `meta.topstruct.metadataName`. If one or more metadata are found, then the named vocabulary is searched. If the vocabulary does not contain an entry with field specifed by `target` which has a value equal to the value of the metadata found, then a new vocabulary entry is created with the `target` field given the value of the found metadata. If there are `generate` fields in the `<item>`, then the fields of the new vocabulary entry specified there are given the values defined in the `content` attributes.


## Configuration
The plugin is configured in the file `plugin_intranda_step_vocabulary_enrichment.xml` as shown here:

{{CONFIG_CONTENT}}

{{CONFIG_DESCRIPTION_PROJECT_STEP}}

Parameter               | Explanation
------------------------|------------------------------------
|`item`| Each metadata which is to be added to a vocabulary is defined in an item. |
|`source`   |This defines the metadata field from which to take the vocabulary entry.    |
|`vocabulary`   | The name of the vocabulary to enrich.  |
|` target`  | The name of the field in the vocabulary which is to be given the value in the source. |
|`generate`   | These are optional fields: if they are specified, then each field in the vocabulary entry which is generated which specifed by the attribute `field` is given the value specified by the attribute `content`.  |