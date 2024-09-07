---
title: Vokabularanreicherung
identifier: intranda_step_vocabulary_enrichment
description: Step Plugin für die automatische Anreicherung von Vokabularen
published: true
---

## Einführung
Diese Dokumentation bietet einen kurzen Überblick über dieses Plugin, das verwendet wird, um Vokabulare basierend auf Metadaten, die in METS-MODS-Dateien innerhalb von Goobi-Workflows gefunden werden, anzureichern.

## Installation
Um das Plugin nutzen zu können, müssen folgende Dateien installiert werden:

```
/opt/digiverso/goobi/plugins/step/plugin_intranda_step_vocabulary_enrichment.jar
/opt/digiverso/goobi/config/plugin_intranda_step_vocabulary_enrichment.xml
```

Sobald das Plugin installiert ist, kann es innerhalb des Workflows für die jeweiligen Arbeitsschritte ausgewählt und somit automatisch ausgeführt werden. 


## Überblick und Funktionalität
Das Programm untersucht die Metadatenfelder einer METS-MODS-Datei aus einem Goobi-Prozess. Für jedes in der Konfigurationsdatei definierte `<item>` durchsucht das Plugin die METS-MODS-Datei nach den angegebenen Metadaten. Die Suche kann auf die oberste Struktur eines Dokuments beschränkt werden, indem die Form `meta.topstruct.metadataName` verwendet wird. Wenn ein oder mehrere Metadaten gefunden werden, wird das benannte Vokabular durchsucht. Wenn das Vokabular keinen Eintrag mit dem im `target`-Feld spezifizierten Wert enthält, der mit dem Wert der gefundenen Metadaten übereinstimmt, wird ein neuer Vokabulareintrag erstellt, bei dem das `target`-Feld den Wert der gefundenen Metadaten erhält. Wenn es `generate`-Felder im `<item>` gibt, werden die Felder des neuen Vokabulareintrags, die dort angegeben sind, mit den im `content`-Attribut definierten Werten versehen.


## Konfiguration
Das Plugin wird in der Datei `plugin_intranda_step_vocabulary_enrichment.xml` wie folgt konfiguriert:

{{CONFIG_CONTENT}}

{{CONFIG_DESCRIPTION_PROJECT_STEP}}

Parameter               | Erklärung
------------------------|------------------------------------
|`item` | Jedes Metadatum, das zu einem Vokabular hinzugefügt werden soll, wird in einem Item definiert. |
|`source`   | Dies definiert das Metadatenfeld, aus dem der Vokabulareintrag entnommen werden soll.    |
|`vocabulary`   | Der Name des zu ergänzenden Vokabulars.  |
|` target`  | Der Name des Feldes im Vokabular, das den Wert der Quelle erhalten soll. |
|`generate`   | Dies sind optionale Felder: Wenn sie angegeben sind, erhält jedes Feld im generierten Vokabulareintrag, das durch das Attribut `field` spezifiziert ist, den im Attribut `content` angegebenen Wert.  |
