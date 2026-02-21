package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Request body for {@code POST /api/v1/extension/connected-goods/mint/batch}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MintConnectedGoodsRequest {

    private List<Item> items;
    private String address;

    @JsonProperty("token_name")
    private String tokenName;

    public MintConnectedGoodsRequest() {}

    public MintConnectedGoodsRequest(String address, String tokenName, Item... items) {
        this.address = address;
        this.tokenName = tokenName;
        this.items = Arrays.asList(items);
    }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTokenName() { return tokenName; }
    public void setTokenName(String tokenName) { this.tokenName = tokenName; }

    /**
     * A single item to be minted in the batch.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        private String password;

        @JsonProperty("asset_name")
        private String assetName;

        public Item() {}

        public Item(String password, String assetName) {
            this.password = password;
            this.assetName = assetName;
        }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getAssetName() { return assetName; }
        public void setAssetName(String assetName) { this.assetName = assetName; }
    }
}
