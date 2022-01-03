package cto.hmi.broker.util;

import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomJsonSerializer<T> implements Serializer<T> {
	private ObjectMapper mapper = new ObjectMapper();
	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
	}

	@Override
	public byte[] serialize(String topic, T data) {
		if (data == null) {
			return null;
		}

		try {
			return mapper.writeValueAsBytes(data);
		} catch (JsonProcessingException | RuntimeException e) {
			throw new SerializationException("error serializing message to JSON ", e);
		}
	}

	@Override
	public void close() {
	}
}