// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class OsmosnimkiOffsetProvider extends OffsetProvider {

    public OsmosnimkiOffsetProvider(String name, String url) {
        super(name, url);
    }

    public OsmosnimkiOffsetProvider(String name, String url, String cookies) {
        super(name, url, cookies);
    }

    @Override
    ProviderType getProviderType() {
        return ProviderType.OSMOSNIMKI;
    }

    @Override
    boolean isLayerSupported(ImageryInfo info) {
        try {
            URL url = new URL(this.url + "action=CheckAvailability&id=" + URLEncoder.encode(info.url, "UTF-8"));
            final BufferedReader rdr = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream(), "UTF-8"));
            if (rdr.readLine().contains("\"offsets_available\": true")) return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    EastNorth getOffset(ImageryInfo info, EastNorth en) {
        LatLon ll = Main.proj.eastNorth2latlon(en);
        try {
            URL url = new URL(this.url + "action=GetOffsetForPoint&lat=" + ll.lat() + "&lon=" + ll.lon() + "&id=" + URLEncoder.encode(info.url, "UTF-8"));
            final BufferedReader rdr = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream(), "UTF-8"));
            String s = rdr.readLine();
            int i = s.indexOf(',');
            if (i == -1) return null;
            String sLat = s.substring(1,i);
            String sLon = s.substring(i,s.length()-1);
            return en.sub(Main.proj.latlon2eastNorth(new LatLon(Double.valueOf(sLat),Double.valueOf(sLon))));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
