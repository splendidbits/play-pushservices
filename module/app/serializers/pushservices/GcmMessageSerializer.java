package serializers.pushservices;

import com.google.gson.*;
import models.pushservices.db.Message;
import models.pushservices.db.PayloadElement;
import models.pushservices.db.Recipient;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.List;

public class GcmMessageSerializer implements JsonSerializer<Message> {
    private JsonArray mJsonRegistrationIds = new JsonArray();

    public GcmMessageSerializer(@Nonnull List<Recipient> recipients) {
        for (Recipient recipient : recipients) {

            // Extra safeguard to ensure we don't add invalid recipients.
            JsonPrimitive jsonRecipientToken = new JsonPrimitive(recipient.getToken() != null ? recipient.getToken() : "");
            mJsonRegistrationIds.add(jsonRecipientToken);
        }
    }

    @Override
    public JsonElement serialize(Message message, Type typeOfSrc, JsonSerializationContext context) {

        // Serialise the payload data into Json Elements.
        JsonObject jsonPayloadData = new JsonObject();
        if (message.getPayloadData() != null) {
            for (PayloadElement payloadElement : message.getPayloadData()) {
                jsonPayloadData.add(payloadElement.getKey(), new JsonPrimitive(payloadElement.getValue()));
            }
        }

        // Serialise the main elements.
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.add("collapse_key", new JsonPrimitive(message.getCollapseKey()));
        jsonMessage.add("time_to_live", new JsonPrimitive(message.getTtlSeconds()));
        jsonMessage.add("dry_run", new JsonPrimitive(message.isDryRun()));
        jsonMessage.add("registration_ids", mJsonRegistrationIds);
        jsonMessage.add("data", jsonPayloadData);

        // Add a restricted package attribute.
        if (message.getCredentials() != null && message.getCredentials().getPackageUri() != null) {
            jsonMessage.add("restricted_package_name", new JsonPrimitive(message.getCredentials().getPackageUri()));
        }

        return jsonMessage;
    }
}