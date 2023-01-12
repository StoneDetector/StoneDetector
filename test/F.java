class F{
//-----Function Pair=1=-----==

selected,1223584.java,61,79,selected,1736762.java,60,78
public static IMarker addMarker (IFile file, String elementId, String location, String message, int statusSeverity) {
    IMarker marker = null;
    try {
        marker = file.createMarker (MARKER_TYPE);
        marker.setAttribute (IMarker.MESSAGE, message);
        marker.setAttribute (IMarker.LOCATION, location);
        marker.setAttribute (org.eclipse.gmf.runtime.common.ui.resources.IMarker.ELEMENT_ID, elementId);
        int markerSeverity = IMarker.SEVERITY_INFO;
        if (statusSeverity == IStatus.WARNING) {
            markerSeverity = IMarker.SEVERITY_WARNING;
        }
        else if (statusSeverity == IStatus.ERROR || statusSeverity == IStatus.CANCEL) {
            markerSeverity = IMarker.SEVERITY_ERROR;
        }
        marker.setAttribute (IMarker.SEVERITY, markerSeverity);
    } catch (CoreException e) {
        SaveccmDiagramEditorPlugin.getInstance ().logError ("Failed to create validation marker", e);
    }
    return marker;
}


public static IMarker addMarker1 (IFile file, String elementId, String location, String message, int statusSeverity) {
    IMarker marker = null;
    try {
        marker = file.createMarker (MARKER_TYPE);
        marker.setAttribute (org.eclipse.core.resources.IMarker.MESSAGE, message);
        marker.setAttribute (org.eclipse.core.resources.IMarker.LOCATION, location);
        marker.setAttribute (org.eclipse.gmf.runtime.common.ui.resources.IMarker.ELEMENT_ID, elementId);
        int markerSeverity = org.eclipse.core.resources.IMarker.SEVERITY_INFO;
        if (statusSeverity == org.eclipse.core.runtime.IStatus.WARNING) {
            markerSeverity = org.eclipse.core.resources.IMarker.SEVERITY_WARNING;
        }
        else if (statusSeverity == org.eclipse.core.runtime.IStatus.ERROR || statusSeverity == org.eclipse.core.runtime.IStatus.CANCEL) {
            markerSeverity = org.eclipse.core.resources.IMarker.SEVERITY_ERROR;
        }
        marker.setAttribute (org.eclipse.core.resources.IMarker.SEVERITY, markerSeverity);
    } catch (CoreException e) {
        M3ActionsDiagramEditorPlugin.getInstance ().logError ("Failed to create validation marker", e);
    }
    return marker;
}

}
