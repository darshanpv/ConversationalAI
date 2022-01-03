package cto.hmi.broker.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import cto.hmi.broker.constants.Params;

public class CustomPartitioner implements Partitioner {

	@Override
	public void configure(Map<String, ?> configs) {

	}

	@Override
	public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {

		// Integer keyInt = Integer.parseInt((String) key);
		Integer keyInt = getKey((String) key);
		return keyInt % Integer.parseInt(String.valueOf(Params.getParam("PARTITIONS")));
	}

	@Override
	public void close() {

	}

	public int getKey(String key) {

		Pattern p = Pattern.compile("\\d+-");
		Matcher m = p.matcher(key);
		if (m.find())
			return Integer.parseInt((String) m.group(0).subSequence(0, m.group(0).length() - 1));
		else
			return 0;
	}

}