// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Comparator;

/**
 * Provides some node order , based on coordinates, nodes with equal coordinates are equal.
 * @author viesturs
 *
 */
public class NodePositionComparator implements Comparator<Node> {

    @Override
    public int compare(Node n1, Node n2) {

        double dLat = n1.getCoor().lat() - n2.getCoor().lat();
        double dLon = n1.getCoor().lon() - n2.getCoor().lon();

        if (dLat > 0)
            return 1;
        else if (dLat < 0)
            return -1;
        else if (dLon == 0) //dlat is 0 here
            return 0;
        else
            return dLon > 0 ? 1 : -1;
    }
}
