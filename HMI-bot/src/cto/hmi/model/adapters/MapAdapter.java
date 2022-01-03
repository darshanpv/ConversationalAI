package cto.hmi.model.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class MapAdapter extends XmlAdapter<MapAdapter.AdaptedMap, Map<String, String>>{

    public static class AdaptedMap {
        public List<Item> item = new ArrayList<Item>();
    }

    public static class Item {
        @XmlAttribute String key;
        @XmlValue String value;
    }
    @Override
    public AdaptedMap marshal(Map<String, String> map) throws Exception {
        AdaptedMap adaptedMap = new AdaptedMap();
        if(map!=null){
	        for(Entry<String, String> entry : map.entrySet()) {
	            Item item = new Item();
	            item.key = entry.getKey();
	            item.value = entry.getValue();
	            adaptedMap.item.add(item);
	        }
	        return adaptedMap;
        }
        return null;
    }

    @Override
    public Map<String, String> unmarshal(AdaptedMap adaptedMap) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        for(Item item : adaptedMap.item) {
            map.put(item.key, item.value);
        }
        return map;
    }

}