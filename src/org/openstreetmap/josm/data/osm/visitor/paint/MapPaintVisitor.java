/* License: GPL. Copyright 2007 by Immanuel Scholz and others */
package org.openstreetmap.josm.data.osm.visitor.paint;

/* To enable debugging or profiling remove the double / signs */

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.IconElemStyle;
import org.openstreetmap.josm.gui.mappaint.LineElemStyle;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.SimpleNodeElemStyle;

public class MapPaintVisitor implements PaintVisitor {

    private Graphics2D g;
    private NavigatableComponent nc;

    private boolean zoomLevelDisplay;
    private boolean drawMultipolygon;
    private boolean drawRestriction;
    private boolean leftHandTraffic;
    private ElemStyles.StyleSet styles;
    private double circum;
    private double dist;
    private static int paintid = 0;
    private EastNorth minEN;
    private EastNorth maxEN;
    private MapPainter painter;
    private MapPaintSettings paintSettings;

    private boolean inactive;

    protected boolean isZoomOk(ElemStyle e) {
        if (!zoomLevelDisplay) /* show everything if the user wishes so */
            return true;

        if(e == null) /* the default for things that don't have a rule (show, if scale is smaller than 1500m) */
            return (circum < 1500);

        return !(circum >= e.maxScale || circum < e.minScale);
    }

    public ElemStyle getPrimitiveStyle(OsmPrimitive osm, boolean nodefault) {
        if(osm.mappaintStyle == null)
        {
            if(styles != null) {
                osm.mappaintStyle = styles.get(osm);
                if(osm instanceof Way) {
                    ((Way)osm).isMappaintArea = styles.isArea(osm);
                }
            }
            if (osm.mappaintStyle == null) {
                if(osm instanceof Node)
                    osm.mappaintStyle = SimpleNodeElemStyle.INSTANCE;
                else if (osm instanceof Way)
                    osm.mappaintStyle = LineElemStyle.UNTAGGED_WAY;
            }
        }
        if(nodefault && osm.mappaintStyle == LineElemStyle.UNTAGGED_WAY)
            return null;
        return osm.mappaintStyle;
    }

    public IconElemStyle getPrimitiveNodeStyle(OsmPrimitive osm) {
        if(osm.mappaintStyle == null && styles != null)
            osm.mappaintStyle = styles.getIcon(osm);

        return (IconElemStyle)osm.mappaintStyle;
    }

    public boolean isPrimitiveArea(Way osm) {
        if(osm.mappaintStyle == null && styles != null) {
            osm.mappaintStyle = styles.get(osm);
            osm.isMappaintArea = styles.isArea(osm);
        }
        return osm.isMappaintArea;
    }

    public void drawNode(Node n) {
        /* check, if the node is visible at all */
        EastNorth en = n.getEastNorth();
        if((en.east()  > maxEN.east() ) ||
                (en.north() > maxEN.north()) ||
                (en.east()  < minEN.east() ) ||
                (en.north() < minEN.north()))
            return;

        ElemStyle nodeStyle = getPrimitiveStyle(n, false);

        if (isZoomOk(nodeStyle)) {
            nodeStyle.paintPrimitive(n, paintSettings, painter, data.isSelected(n),
            false);
        }
    }

    public void drawWay(Way w, int fillAreas) {
        if(w.getNodesCount() < 2)
            return;

        if (w.hasIncompleteNodes())
            return;

        /* check, if the way is visible at all */
        double minx = 10000;
        double maxx = -10000;
        double miny = 10000;
        double maxy = -10000;

        for (Node n : w.getNodes())
        {
            if(n.getEastNorth().east() > maxx) {
                maxx = n.getEastNorth().east();
            }
            if(n.getEastNorth().north() > maxy) {
                maxy = n.getEastNorth().north();
            }
            if(n.getEastNorth().east() < minx) {
                minx = n.getEastNorth().east();
            }
            if(n.getEastNorth().north() < miny) {
                miny = n.getEastNorth().north();
            }
        }

        if ((minx > maxEN.east()) ||
                (miny > maxEN.north()) ||
                (maxx < minEN.east()) ||
                (maxy < minEN.north()))
            return;

        ElemStyle wayStyle = getPrimitiveStyle(w, false);

        if(!isZoomOk(wayStyle))
            return;

        if(wayStyle instanceof LineElemStyle) {
            wayStyle.paintPrimitive(w, paintSettings, painter, data.isSelected(w), false);
        } else if (wayStyle instanceof AreaElemStyle) {
            AreaElemStyle areaStyle = (AreaElemStyle) wayStyle;
            /* way with area style */
            if (fillAreas > dist)
            {
                painter.drawArea(getPolygon(w), (data.isSelected(w) ? paintSettings.getSelectedColor() : areaStyle.color), painter.getAreaName(w));
            }
            areaStyle.getLineStyle().paintPrimitive(w, paintSettings, painter, data.isSelected(w), false);
        }
    }

    public void paintUnselectedRelation(Relation r) {
        if (drawMultipolygon && "multipolygon".equals(r.get("type")))
            drawMultipolygon(r);
        else if (drawRestriction && "restriction".equals(r.get("type")))
            drawRestriction(r);
    }

    public void drawRestriction(Relation r) {

        Way fromWay = null;
        Way toWay = null;
        OsmPrimitive via = null;

        /* find the "from", "via" and "to" elements */
        for (RelationMember m : r.getMembers())
        {
            if(m.getMember().isIncomplete())
                return;
            else
            {
                if(m.isWay())
                {
                    Way w = m.getWay();
                    if(w.getNodesCount() < 2) {
                        continue;
                    }

                    if("from".equals(m.getRole())) {
                        if(fromWay == null)
                            fromWay = w;
                    } else if("to".equals(m.getRole())) {
                        if(toWay == null)
                            toWay = w;
                    } else if("via".equals(m.getRole())) {
                        if(via == null)
                            via = w;
                    }
                }
                else if(m.isNode())
                {
                    Node n = m.getNode();
                    if("via".equals(m.getRole()) && via == null)
                        via = n;
                }
            }
        }

        if (fromWay == null || toWay == null || via == null)
            return;

        Node viaNode;
        if(via instanceof Node)
        {
            viaNode = (Node) via;
            if(!fromWay.isFirstLastNode(viaNode)) {
                return;
            }
        }
        else
        {
            Way viaWay = (Way) via;
            Node firstNode = viaWay.firstNode();
            Node lastNode = viaWay.lastNode();
            Boolean onewayvia = false;

            String onewayviastr = viaWay.get("oneway");
            if(onewayviastr != null)
            {
                if("-1".equals(onewayviastr)) {
                    onewayvia = true;
                    Node tmp = firstNode;
                    firstNode = lastNode;
                    lastNode = tmp;
                } else {
                    onewayvia = OsmUtils.getOsmBoolean(onewayviastr);
                    if (onewayvia == null) {
                        onewayvia = false;
                    }
                }
            }

            if(fromWay.isFirstLastNode(firstNode)) {
                viaNode = firstNode;
            } else if (!onewayvia && fromWay.isFirstLastNode(lastNode)) {
                viaNode = lastNode;
            } else {
                return;
            }
        }

        /* find the "direct" nodes before the via node */
        Node fromNode = null;
        if(fromWay.firstNode() == via) {
            fromNode = fromWay.getNode(1);
        } else {
            fromNode = fromWay.getNode(fromWay.getNodesCount()-2);
        }

        Point pFrom = nc.getPoint(fromNode);
        Point pVia = nc.getPoint(viaNode);

        /* starting from via, go back the "from" way a few pixels
           (calculate the vector vx/vy with the specified length and the direction
           away from the "via" node along the first segment of the "from" way)
         */
        double distanceFromVia=14;
        double dx = (pFrom.x >= pVia.x) ? (pFrom.x - pVia.x) : (pVia.x - pFrom.x);
        double dy = (pFrom.y >= pVia.y) ? (pFrom.y - pVia.y) : (pVia.y - pFrom.y);

        double fromAngle;
        if(dx == 0.0) {
            fromAngle = Math.PI/2;
        } else {
            fromAngle = Math.atan(dy / dx);
        }
        double fromAngleDeg = Math.toDegrees(fromAngle);

        double vx = distanceFromVia * Math.cos(fromAngle);
        double vy = distanceFromVia * Math.sin(fromAngle);

        if(pFrom.x < pVia.x) {
            vx = -vx;
        }
        if(pFrom.y < pVia.y) {
            vy = -vy;
        }

        /* go a few pixels away from the way (in a right angle)
           (calculate the vx2/vy2 vector with the specified length and the direction
           90degrees away from the first segment of the "from" way)
         */
        double distanceFromWay=10;
        double vx2 = 0;
        double vy2 = 0;
        double iconAngle = 0;

        if(pFrom.x >= pVia.x && pFrom.y >= pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg - 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 90));
            }
            iconAngle = 270+fromAngleDeg;
        }
        if(pFrom.x < pVia.x && pFrom.y >= pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg));
            } else {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 180));
            }
            iconAngle = 90-fromAngleDeg;
        }
        if(pFrom.x < pVia.x && pFrom.y < pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg - 90));
            }
            iconAngle = 90+fromAngleDeg;
        }
        if(pFrom.x >= pVia.x && pFrom.y < pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 180));
            } else {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg));
            }
            iconAngle = 270-fromAngleDeg;
        }

        IconElemStyle nodeStyle = getPrimitiveNodeStyle(r);

        if (nodeStyle == null) {
            return;
        }

        painter.drawRestriction(inactive || r.isDisabled() ? nodeStyle.getDisabledIcon() : nodeStyle.icon,
                pVia, vx, vx2, vy, vy2, iconAngle, data.isSelected(r));
    }

    public boolean drawMultipolygon(Relation r) {
        boolean drawn = false;

        Multipolygon multipolygon = new Multipolygon(nc);
        multipolygon.load(r);

        ElemStyle wayStyle = getPrimitiveStyle(r, false);

        boolean disabled = r.isDisabled();
        // If area style was not found for relation then use style of ways
        if(styles != null && !(wayStyle instanceof AreaElemStyle)) {
            for (Way w : multipolygon.getOuterWays()) {
                wayStyle = styles.getArea(w);
                disabled = disabled || w.isDisabled();
                if(wayStyle != null) {
                    break;
                }
            }
        }

        if (wayStyle instanceof AreaElemStyle) {
            boolean zoomok = isZoomOk(wayStyle);
            boolean visible = false;

            drawn = true;

            if(zoomok && !disabled && !multipolygon.getOuterWays().isEmpty()) {
                AreaElemStyle areaStyle = (AreaElemStyle)wayStyle;
                for (PolyData pd : multipolygon.getCombinedPolygons()) {
                    Polygon p = pd.get();
                    if(!isPolygonVisible(p)) {
                        continue;
                    }

                    boolean selected = pd.selected || data.isSelected(r);
                    painter.drawArea(p, selected ? paintSettings.getRelationSelectedColor()
                    : areaStyle.color, painter.getAreaName(r));
                    visible = true;
                }
            }

            if(!visible)
                return drawn;
            for (Way wInner : multipolygon.getInnerWays()) {
                ElemStyle innerStyle = getPrimitiveStyle(wInner, true);
                if(innerStyle == null) {
                    if (data.isSelected(wInner) || disabled)
                        continue;
                    if(zoomok && (wInner.mappaintDrawnCode != paintid || multipolygon.getOuterWays().isEmpty())) {
                        ((AreaElemStyle)wayStyle).getLineStyle().paintPrimitive(wInner, paintSettings,
                        painter, (data.isSelected(wInner) || data.isSelected(r)), false);
                    }
                    wInner.mappaintDrawnCode = paintid;
                }
                else
                {
                    if(wayStyle.equals(innerStyle))
                    {
                        wInner.mappaintDrawnAreaCode = paintid;
                        if(!data.isSelected(wInner))
                        {
                            wInner.mappaintDrawnCode = paintid;
                            drawWay(wInner, 0);
                        }
                    }
                }
            }
            for (Way wOuter : multipolygon.getOuterWays()) {
                ElemStyle outerStyle = getPrimitiveStyle(wOuter, true);
                if(outerStyle == null) {
                    // Selected ways are drawn at the very end
                    if (data.isSelected(wOuter))
                        continue;
                    if(zoomok) {
                        ((AreaElemStyle)wayStyle).getLineStyle().paintPrimitive(wOuter, paintSettings, painter,
                        (data.isSelected(wOuter) || data.isSelected(r)), r.isSelected());
                    }
                    wOuter.mappaintDrawnCode = paintid;
                } else if(outerStyle instanceof AreaElemStyle) {
                    wOuter.mappaintDrawnAreaCode = paintid;
                    if(!data.isSelected(wOuter)) {
                        wOuter.mappaintDrawnCode = paintid;
                        drawWay(wOuter, 0);
                    }
                }
            }
        }
        return drawn;
    }

    protected boolean isPolygonVisible(Polygon polygon) {
        Rectangle bounds = polygon.getBounds();
        if (bounds.width == 0 && bounds.height == 0) return false;
        if (bounds.x > nc.getWidth()) return false;
        if (bounds.y > nc.getHeight()) return false;
        if (bounds.x + bounds.width < 0) return false;
        if (bounds.y + bounds.height < 0) return false;
        return true;
    }

    protected Polygon getPolygon(Way w)
    {
        Polygon polygon = new Polygon();

        for (Node n : w.getNodes())
        {
            Point p = nc.getPoint(n);
            polygon.addPoint(p.x,p.y);
        }
        return polygon;
    }

    protected Point2D getCentroid(Polygon p)
    {
        double cx = 0.0, cy = 0.0, a = 0.0;

        // usually requires points[0] == points[npoints] and can then use i+1 instead of j.
        // Faked it slowly using j.  If this is really gets used, this should be fixed.
        for (int i = 0;  i < p.npoints;  i++) {
            int j = i+1 == p.npoints ? 0 : i+1;
            a += (p.xpoints[i] * p.ypoints[j]) - (p.ypoints[i] * p.xpoints[j]);
            cx += (p.xpoints[i] + p.xpoints[j]) * (p.xpoints[i] * p.ypoints[j] - p.ypoints[i] * p.xpoints[j]);
            cy += (p.ypoints[i] + p.ypoints[j]) * (p.xpoints[i] * p.ypoints[j] - p.ypoints[i] * p.xpoints[j]);
        }
        return new Point2D.Double(cx / (3.0*a), cy / (3.0*a));
    }

    protected double getArea(Polygon p)
    {
        double sum = 0.0;

        // usually requires points[0] == points[npoints] and can then use i+1 instead of j.
        // Faked it slowly using j.  If this is really gets used, this should be fixed.
        for (int i = 0;  i < p.npoints;  i++) {
            int j = i+1 == p.npoints ? 0 : i+1;
            sum = sum + (p.xpoints[i] * p.ypoints[j]) - (p.ypoints[i] * p.xpoints[j]);
        }
        return Math.abs(sum/2.0);
    }

    DataSet data;

    <T extends OsmPrimitive> Collection<T> selectedLast(final DataSet data, Collection <T> prims) {
        ArrayList<T> sorted = new ArrayList<T>(prims);
        Collections.sort(sorted,
                new Comparator<T>() {
            public int compare(T o1, T o2) {
                boolean s1 = data.isSelected(o1);
                boolean s2 = data.isSelected(o2);
                if (s1 && !s2)
                    return 1;
                if (!s1 && s2)
                    return -1;
                return o1.compareTo(o2);
            }
        });
        return sorted;
    }

    /* Shows areas before non-areas */
    public void visitAll(DataSet data, boolean virtual, Bounds bounds) {
        BBox bbox = new BBox(bounds);
        this.data = data;
        ++paintid;

        int fillAreas = Main.pref.getInteger("mappaint.fillareas", 10000000);
        LatLon ll1 = nc.getLatLon(0, 0);
        LatLon ll2 = nc.getLatLon(100, 0);
        dist = ll1.greatCircleDistance(ll2);

        zoomLevelDisplay = Main.pref.getBoolean("mappaint.zoomLevelDisplay", false);
        circum = nc.getDist100Pixel();
        styles = MapPaintStyles.getStyles().getStyleSet();
        drawMultipolygon = Main.pref.getBoolean("mappaint.multipolygon", true);
        drawRestriction = Main.pref.getBoolean("mappaint.restriction", true);
        leftHandTraffic = Main.pref.getBoolean("mappaint.lefthandtraffic", false);
        minEN = nc.getEastNorth(0, nc.getHeight() - 1);
        maxEN = nc.getEastNorth(nc.getWidth() - 1, 0);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Main.pref.getBoolean("mappaint.use-antialiasing", false) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        this.paintSettings = MapPaintSettings.INSTANCE;
        this.painter = new MapPainter(paintSettings, g, inactive, nc, virtual, dist, circum);

        if (fillAreas > dist && styles != null && styles.hasAreas()) {
            Collection<Way> noAreaWays = new LinkedList<Way>();
            final Collection<Way> ways = data.searchWays(bbox);

            /*** disabled ***/
            for (final Way osm : ways) {
                if (osm.isDisabled() && osm.isDrawable() && osm.mappaintDrawnCode != paintid) {
                    drawWay(osm, 0);
                    osm.mappaintDrawnCode = paintid;
                }
            }

            /*** RELATIONS ***/
            for (final Relation osm: data.searchRelations(bbox)) {
                if (osm.isDrawable()) {
                    paintUnselectedRelation(osm);
                }
            }

            /*** AREAS ***/
            for (final Way osm : selectedLast(data, ways)) {
                if (osm.isDrawable() && osm.mappaintDrawnCode != paintid) {
                    if (isPrimitiveArea(osm)) {
                        if(osm.mappaintDrawnAreaCode != paintid)
                            drawWay(osm, fillAreas);
                    } else if(!data.isSelected(osm)) {
                        noAreaWays.add(osm);
                    }
                }
            }

            /*** WAYS ***/
            for (final Way osm : noAreaWays) {
                drawWay(osm, 0);
                osm.mappaintDrawnCode = paintid;
            }
        } else {
            drawMultipolygon = false;
            final Collection<Way> ways = data.searchWays(bbox);

            /*** WAYS (disabled)  ***/
            for (final Way way: ways) {
                if (way.isDisabled() && way.isDrawable() && !data.isSelected(way)) {
                    drawWay(way, 0);
                    way.mappaintDrawnCode = paintid;
                }
            }

            /*** RELATIONS ***/
            for (final Relation osm: data.searchRelations(bbox)) {
                if (osm.isDrawable()) {
                    paintUnselectedRelation(osm);
                }
            }

            /*** WAYS (filling disabled)  ***/
            for (final Way way: ways) {
                if (way.isDrawable() && !data.isSelected(way)) {
                    drawWay(way, 0);
                }
            }
        }

        /*** SELECTED  ***/
        for (final OsmPrimitive osm : data.getSelected()) {
            if (osm.isUsable() && !(osm instanceof Node) && (osm instanceof Relation || osm.mappaintDrawnCode != paintid)) {
                osm.visit(new AbstractVisitor() {
                    public void visit(Way w) {
                        drawWay(w, 0);
                    }

                    public void visit(Node n) {
                        // Selected nodes are painted in following part
                    }

                    public void visit(Relation r) {
                        for (RelationMember m : r.getMembers()) {
                            OsmPrimitive osm = m.getMember();
                            if(osm.isDrawable()) {
                                ElemStyle style = getPrimitiveStyle(m.getMember(), false);
                                if(osm instanceof Way)
                                {
                                    if(style instanceof AreaElemStyle) {
                                        ((AreaElemStyle)style).getLineStyle().paintPrimitive(osm, paintSettings, painter, true, true);
                                    } else {
                                        style.paintPrimitive(osm, paintSettings, painter, true, true);
                                    }
                                }
                                else if(osm instanceof Node)
                                {
                                    if(isZoomOk(style)) {
                                        style.paintPrimitive(osm, paintSettings, painter, true, true);
                                    }
                                }
                                osm.mappaintDrawnCode = paintid;
                            }
                        }
                    }
                });
            }
        }

        /*** NODES ***/
        for (final Node osm: data.searchNodes(bbox)) {
            if (!osm.isIncomplete() && !osm.isDeleted() && (data.isSelected(osm) || !osm.isDisabledAndHidden())
                    && osm.mappaintDrawnCode != paintid) {
                drawNode(osm);
            }
        }

        painter.drawVirtualNodes(data.searchWays(bbox));
    }

    public void setGraphics(Graphics2D g) {
        this.g = g;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public void setNavigatableComponent(NavigatableComponent nc) {
        this.nc = nc;
    }
}
