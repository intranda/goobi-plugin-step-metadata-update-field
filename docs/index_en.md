---
title: Update Metadata Fields
identifier: intranda_step_metadata_update_field
description: Step Plugin for automatically updating values in METS files
published: true
---

## Introduction
This Step plugin for Goobi workflow allows to automatically create or update specific metadata fields inside of METS files. To do so it can use the Variable Replacer or neighbor metadata fields to write metadata to logical elements on all hiearchical levels.

## Installation
To be able to use the plugin, the following files must be installed:

```bash
/opt/digiverso/goobi/plugins/step/plugin-step-metadata-update-field-base.jar
/opt/digiverso/goobi/config/plugin_intranda_step_metadata_update_field.xml
```

After installing the plugin, it can be selected within the workflow for the respective work steps and thus executed automatically.

To use the plugin, it must be selected in a work step:

![Configuration of the workflow step for using the plugin](screen1_en.png)


## Overview and functionality
First, the values that the plugin is supposed to update must be defined in the configuration file. When the plugin is executed, it collects all relevant structural elements of the METS file. It then checks whether and how the specified values should be updated. If the conditions are met, either new values are inserted into empty fields, or existing values are overwritten if this is forced.


## Configuration
The plugin is configured in the file `plugin_intranda_step_metadata_update_field.xml` as shown here:

{{CONFIG_CONTENT}}

{{CONFIG_DESCRIPTION_PROJECT_STEP}}

Parameter               | Explanation
------------------------|------------------------------------
`<field>`      | Defines the field within the METS file for which the content should be generated. |
`<element>`    | Specifies the structural elements for which the content should be updated. Multiple element entries can be listed here. Use `*` to match all structural element types. |
`<forceUpdate>`| Indicates whether the content should be overwritten if the field is not empty. |
`variable`     | Analyzed and replaced by the variable replacer. |
`metadata`     | Uses the value of the metadata field with the given name inside the same structural element. |
`static`       | Uses a static string. |
`random`       | Generates a random number with a defined length. |
`uuid`         | Uses a UUID (Universally Unique Identifier) with 36 characters. |
`timestamp`    | Generates a numeric timestamp. |
`counter`      | Generates a sequential number that is automatically incremented. For example, using `%03d` will count as follows: `001`, `002`, `003`, etc. |
`groupcounter` | A separate counter for each value of the `groupField` is used as the content.  |
`<replace>`    | Allows text replacement, where specified texts are replaced with others. |