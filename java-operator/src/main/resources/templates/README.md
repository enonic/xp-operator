# Templates

The operator renders these files into the resources of a deployment. They are [Pebble](https://pebbletemplates.io/) templates
that produce YAML, rendered in the operator's own process by `TemplateRenderer`. `xp8deployment.yaml` is the entry point: it
includes the other files, so a new file only has to be added there.

| Syntax | Result |
|---|---|
| `{{ value }}` | The value as a quoted YAML string. A value can never change the structure of the document. Nothing is written for a missing value, which leaves the field unset; an empty string stays an empty string. |
| `{{ value \| raw }}` | The value as it is. Use it for text inside a larger string. |
| `{{ value \| toJson }}` | A map or a list as JSON, which YAML reads as it stands: `ownerReferences: {{ ownerReferences \| toJson }}`. |
| `{{ value \| default(other) }}` | The other value if the first is missing or empty. |
| `{{ file("name") }}` | The content of a file of this directory, as a string. |
| `{{ imageRef(image, prefix) }}` | The image, behind the registry prefix if it names no registry of its own. |
| `{%- if … %}`, `{%- else %}`, `{%- endif %}` | A condition. |
| `{%- for item in list %}`, `{%- endfor %}` | A loop over a list. |
| `{%- for e in map \| entries %}` | A loop over a map, with `e.key` and `e.value`. |
| `{% include "file.yaml" %}` | The content of another file, which sees the same values and loop variables. An included file is written at the indentation it is used at. |

Write block tags with the dash, `{%- … %}`, on a line of their own: the dash removes the line they stand on.

Templates read maps and lists only, and cannot call methods. The values come from `operator.charts.values.*` in the operator's
configuration and from the resource being rendered, under the same names.
