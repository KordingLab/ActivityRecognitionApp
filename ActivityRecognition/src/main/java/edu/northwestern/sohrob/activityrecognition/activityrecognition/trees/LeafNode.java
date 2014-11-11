package edu.northwestern.sohrob.activityrecognition.activityrecognition.trees;

/**
 * Created by sohrob on 11/10/14.
 */

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates a leaf node that returns a prediction.
 */

public class LeafNode extends TreeNode {
    public static final String PREDICTION = "prediction";
    public static final String ACCURACY = "accuracy";

    private HashMap<String, Object> _prediction;

    /**
     * Construct a new leaf node and assign its prediction.
     *
     * @param prediction
     *            Prediction containing at least a LeafNode.PREDICTION key-value
     *            pair as well as a LeafNode.ACCURACY estimate of the accuracy
     *            or confidence of this prediction. Parsers may add additional
     *            keys as needed.
     */

    public LeafNode(String name, HashMap<String, Object> prediction) {

        super(name);

        this._prediction = prediction;

        this._prediction.put(TreeNode.NAME, name); //////////////////???????????????????????????
    }

    public LeafNode(HashMap<String, Object> prediction) {

        super(null);

        this._prediction = prediction;
    }

    /**
     * Returns the prediction associated with this leaf node.
     */

    public Map<String, Object> fetchPrediction(Map<String, Double> features) {

        return this._prediction;

    }

    /**
     * Returns a representation of the leaf node in "PREDICTION (ACCURACY)"
     * format for human consumption.
     */

    public String toString(int indent) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < indent; i++)
            sb.append("  ");

        sb.append(this._prediction.get(LeafNode.PREDICTION).toString());
        sb.append(" (");
        sb.append(_prediction.get(LeafNode.ACCURACY).toString());
        sb.append(")");

        return sb.toString();
    }
}
