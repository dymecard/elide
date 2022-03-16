package elide.driver.redis;

import elide.model.EncodedModel;
import io.lettuce.core.codec.RedisCodec;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/** Provides a Lettuce codec for translating in between [com.google.protobuf.Message] and [EncodedModel] instances. */
public final class RedisEncodedModelCodec implements RedisCodec<String, EncodedModel> {
    private static final Charset charset = StandardCharsets.UTF_8;
    private static final RedisEncodedModelCodec singleton = new RedisEncodedModelCodec();

    /** @return Singleton instance of the Redis object codec. */
    public static RedisEncodedModelCodec acquire() {
        return singleton;
    }

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return charset.decode(bytes).toString();
    }

    @Override
    public EncodedModel decodeValue(ByteBuffer bytes) {
        try {
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(array));
            return (EncodedModel)is.readObject();
        } catch (Exception err) {
            return null;
        }
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return charset.encode(key);
    }

    @Override
    public ByteBuffer encodeValue(EncodedModel value) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bytes);
            os.writeObject(value);
            return ByteBuffer.wrap(bytes.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }
}
