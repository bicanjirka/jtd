package td.ui.render;

/**
 * The symbol a path marker is drawn as. Adding a new symbol - e.g. a second chevron style -
 * is one constant here plus one branch in {@link td.ui.Java2DFrameRenderer}'s shape switch;
 * every {@link PathMarkerDraw} already carries a facing angle regardless of shape, so a
 * direction-sensitive symbol needs no other change.
 */
public enum PathMarkerShape {
    DOT,
    CHEVRON
}
