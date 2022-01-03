package cto.hmi.broker.util;

import java.io.IOException;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import cto.hmi.broker.DialogObject;

public class CustomJsonDeserializer implements Deserializer<Object> {
	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public DialogObject deserialize(String topic, byte[] data) {
		DialogObject dialogObject = null;
		try {
			mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			// mapper.registerModule(new JsonRawValueDeserializerModule());
			dialogObject = mapper.readValue(data, DialogObject.class);

		} catch (IOException | RuntimeException e) {
			throw new SerializationException("error deserializing from JSON message", e);
		}
		return dialogObject;
	}

	@Override
	public void close() {
	}
}