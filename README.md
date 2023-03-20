Structurizr Tools
===

Tools for working with [Structurizr](https://structurizr.com/).

* `merge(List<Workspace> workspaces, Model model)`<br>
Supports the Enterprise-wide use case where separate workspaces are combined to form a mega-workspace.  In order to support merging, the workspaces must follow a simple convention.  The definitive version of each model element must be found in one workspace.  Occurrences of the element in other workspaces must be tagged as 'External'.
