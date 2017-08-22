package appmodels.pushservices;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response model from Google GCM endpoint (for response status 200).
 */
public class GcmResponse {

    @SerializedName("multicast_id")
    public String messageId;

    @SerializedName("success")
    public int successCount;

    @SerializedName("failure")
    public int failCount;

    @SerializedName("canonical_ids")
    public int canonicalIdCount;

    @SerializedName("results")
    public List<ResultData> results;

    public class ResultData {
        @SerializedName("message_id")
        public String messageId;

        @SerializedName("registration_id")
        public String registrationId;

        @SerializedName("error")
        public String error;
    }
}
