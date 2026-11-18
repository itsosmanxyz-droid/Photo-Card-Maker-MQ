TEMPLATE LIBRARY - how it works
===============================
assets/templates/<category folder>/<Template_Name>.json   = one template
assets/templates/<category folder>/category.json          = category title / description / colour

* Add a template : copy any .json file in a category folder, rename it, edit it. Rebuild. Done.
* Add a category : create a new folder (e.g. 10_Logo_Cards) with a category.json inside. Done.
  A category with no template files shows "Coming Soon" automatically.
* Folders are shown in name order (01_, 02_, ...). Colours are written as "#AARRGGBB".
* Positions are fractions of the card (0.0 to 1.0).
* No Java or XML file needs to be edited. Broken .json files are skipped.
