package edu.northwestern.sohrob.activityrecognition.activityrecognition;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.northwestern.sohrob.activityrecognition.activityrecognition.trees.LeafNode;
import edu.northwestern.sohrob.activityrecognition.activityrecognition.trees.TreeNode;
import edu.northwestern.sohrob.activityrecognition.activityrecognition.trees.parsers.TreeNodeParser;

/**
 * Created by sohrob on 11/10/14.
 */
public class RandomForest {

    public static final String TYPE = "matlab-forest";

    private static final String VOTES = "VOTES";
    private static final String TREE_COUNT = "TOTAL_VOTERS";

    private ArrayList<TreeNode> _trees = new ArrayList<TreeNode>();

    public RandomForest() {

    }

    protected void generateModel(Object model) {
        synchronized (this) {
            if (model instanceof JSONArray) {

                JSONArray modelArray = (JSONArray) model;

                for (int i = 0; i < modelArray.length(); i++) {
                    try {
                        Object modelItem = modelArray.get(i);

                        if (modelItem instanceof String) {
                            try {
                                TreeNode tree = TreeNodeParser.parseString(modelItem.toString());
                                this._trees.add(tree);
                            }
                            catch (TreeNodeParser.ParserNotFound e) {
                                e.printStackTrace();
                            }
                            catch (TreeNode.TreeNodeException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected HashMap<String, Object> evaluateModel(Map<String, Double> snapshot) {

        String maxPrediction = null;
        int maxCount = -1;

        //Log.i("info2", snapshot.keySet()+"");

        synchronized (this) {

            Map<String, Integer> counts = new HashMap<String, Integer>();

            for (TreeNode tree : this._trees) {
                try {


                    Map<String, Object> prediction = tree.fetchPrediction(snapshot);

                    String treePrediction = prediction.get(LeafNode.PREDICTION).toString();

                    Integer count = 0;

                    if (counts.containsKey(treePrediction))
                        count = counts.get(treePrediction);

                    count = Integer.valueOf(count.intValue() + 1);
                    counts.put(treePrediction.toString(), count);

                } catch (TreeNode.TreeNodeException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (String prediction : counts.keySet()) {
                Integer count = counts.get(prediction);

                if (count.intValue() > maxCount) {
                    maxCount = count.intValue();
                    maxPrediction = prediction;
                }
            }
        }

        //Log.e("INF", "Class: " + maxPrediction);
        /*if (true) ((maxPrediction!=null)&&(maxPrediction.equals("1"))) {
            try {
                ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 25);
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }*/

        HashMap<String, Object> prediction = new HashMap<String, Object>();
        prediction.put(LeafNode.PREDICTION, maxPrediction);
        prediction.put(LeafNode.ACCURACY, (double) maxCount
                / (double) this._trees.size());
        prediction.put(RandomForest.VOTES, maxCount);
        prediction.put(RandomForest.TREE_COUNT, this._trees.size());

        return prediction;
    }

    public String modelType() {
        return RandomForest.TYPE;
    }



}
