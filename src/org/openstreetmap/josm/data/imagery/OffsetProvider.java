// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.coor.EastNorth;

public abstract class OffsetProvider {
    public static List<OffsetProvider> activeProviders = new ArrayList<OffsetProvider>();
    public static OffsetProvider[] defaultProviders = new OffsetProvider[] {
        new OsmosnimkiOffsetProvider("Osmosnimki", "http://offset.osmosnimki.ru/offset/v0?")
    };

    public enum ProviderType {
        OSMOSNIMKI
    }

    public String name;
    public String url = null;
    public String cookies = null;

    public OffsetProvider(String name, String url) {
        this(name, url, null);
    }

    public OffsetProvider(String name, String url, String cookies) {
        this.name=name;
        this.url = url;
        this.cookies = cookies;
    }

    public ArrayList<String> getInfoArray() {
        ArrayList<String> res = new ArrayList<String>(7);
        res.add(this.name);
        res.add(String.valueOf(this.getProviderType().ordinal()));
        res.add(url);
        if(cookies != null && !cookies.isEmpty()) {
            res.add(cookies);
        }
        return res;
    }

    public static OffsetProvider createOffsetProvider(Collection<String> list) {
        ArrayList<String> array = new ArrayList<String>(list);
        String name = array.get(0);
        String url = array.get(2);
        String cookies = null;
        if (array.size() >= 3) {
            cookies = array.get(2);
        }
        try {
            ProviderType providerType = ProviderType.values()[Integer.valueOf(array.get(1))];
            switch (providerType) {
            case OSMOSNIMKI:
                return new OsmosnimkiOffsetProvider(name, url, cookies);
            }
        } catch (Exception e) {
        }
        return null;
    }

    abstract ProviderType getProviderType();
    abstract boolean isLayerSupported(ImageryInfo info);
    abstract EastNorth getOffset(ImageryInfo info, EastNorth en);
}
