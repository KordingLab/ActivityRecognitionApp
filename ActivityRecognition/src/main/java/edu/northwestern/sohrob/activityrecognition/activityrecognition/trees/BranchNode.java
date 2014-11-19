package edu.northwestern.sohrob.activityrecognition.activityrecognition.trees;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class BranchNode extends TreeNode {

    /**
     * Thrown when a tree is evaluated and a value is not available to match any
     * of the associated conditions.
     */
    public static class MissingValueException extends TreeNodeException {
        public MissingValueException(String message) {
            super(message);
        }

        private static final long serialVersionUID = 7858585916379498941L;
    }

    private ArrayList<Condition> _conditions = new ArrayList<Condition>();

    /**
     * Different kinds of tests and comparisons. (Not all are currently
     * implemented.
     */

    public static enum Operation {
        LESS_THAN, LESS_THAN_OR_EQUAL_TO, MORE_THAN, MORE_THAN_OR_EQUAL_TO, EQUALS, EQUALS_CASE_INSENSITIVE, CONTAINS, CONTAINED_BY, STARTS_WITH, ENDS_WITH, DEFAULT // Always
        // passes.
        // Typically
        // used
        // as
        // a
        // catch-all
        // for
        // missing
        // data
        // scenarios.
    }

    /**
     * Represents a test run against the feature data representing the real
     * world. Conditions consist of a test (feature to test, test operation, and
     * value/threshold) and a "next node" that is returned when the state of the
     * world passes the condition.
     */

    public static class Condition {
        public static final int DEFAULT_PRIORITY = 0;
        public static final int LOWEST_PRIORITY = Integer.MIN_VALUE;
        public static final int HIGHEST_PRIORITY = Integer.MAX_VALUE;

        String _feature = null;
        double _value;

        BranchNode.Operation _operation = Operation.DEFAULT;

        TreeNode _node = null;

        int _priority = Condition.DEFAULT_PRIORITY;

        /**
         * Builds a condition.
         *
         * @param op
         *            Test operation.
         * @param feature
         *            Key of the feature to test.
         * @param value
         *            Test value or threshold.
         * @param priority
         *            Priority of the condition. Conditions with higher priority
         *            are evaluated before those with lower priorities.
         * @param node
         *            TreeNode associated with fulfilling this condition.
         */

        public Condition(Operation op, String feature, double value, int priority, TreeNode node) {
            this._operation = op;
            this._feature = feature;
            this._value = value;
            this._priority = priority;
            this._node = node;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */

        public String toString() {
            StringBuffer sb = new StringBuffer();

            sb.append(this._feature);
            sb.append(" ");
            sb.append(this._operation.name());
            sb.append(" ");
            sb.append(this._value);
            sb.append(" (");
            sb.append(this._priority);
            sb.append(")");

            return sb.toString();
        }

        /**
         * Evaluates the provided features against this condition.
         *
         * @param features
         *            Key-value pairs representing the state of the world.
         * @return True if the features fulfills this condition, false
         *         otherwise.
         *
         * @throws TreeNodeException
         *             Thrown on error evaluating the coditions.
         */

        public boolean evaluate(Map<String, Double> features) throws TreeNodeException {

            Double value = features.get(this._feature);

            //Log.e("info3", features.keySet()+"");
            //Log.e("info4", this._feature);

            if (value == null && this._operation != Operation.DEFAULT) {
                // We're missing a value to test and this isn't a DEFAULT node.
                //Log.e("info", "null value");

                return false;
            }

            switch (this._operation) {
                case LESS_THAN:
                    return Condition.testLessThan(this._value, value);
                case LESS_THAN_OR_EQUAL_TO:
                    return Condition.testLessThanOrEqualTo(this._value, value);
                case MORE_THAN:
                    return Condition.testMoreThan(this._value, value);
                case MORE_THAN_OR_EQUAL_TO:
                    return Condition.testMoreThanOrEqualTo(this._value, value);
                case DEFAULT:
                    return true;
            }

            return false;
        }

        /**
         * Tests if value >= test.
         *
         * @param test
         * @param value
         * @return
         * @throws TreeNodeException
         */

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static boolean testMoreThanOrEqualTo(double test, double value) throws TreeNodeException {

            return (value >= test);
        }

        /**
         * Tests if value <= test.
         *
         * @param test
         * @param value
         * @return
         * @throws TreeNodeException
         */

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static boolean testLessThanOrEqualTo(double test, double value) throws TreeNodeException {

            return (value<=test);
        }

        /**
         * Tests if value > test.
         *
         * @param test
         * @param value
         * @return
         * @throws TreeNodeException
         */

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static boolean testMoreThan(double test, double value) throws TreeNodeException {

            return (value > test);
        }

        /**
         * Tests if value < test.
         *
         * @param test
         * @param value
         * @return
         * @throws TreeNodeException
         */

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static boolean testLessThan(double test, double value) throws TreeNodeException {

            return (value < test);
        }

        /**
         * Returns the node associated with this condition.
         *
         * @return Associated TreeNode
         * @throws TreeNode.TreeNodeException
         *             Thrown if no node is associated with this condition.
         */

        public TreeNode getNode() throws TreeNode.TreeNodeException {
            if (this._node == null)
                throw new TreeNode.TreeNodeException(
                        "Null tree node encountered.");

            return this._node;
        }
    }

    public BranchNode(String name) {
        super(name);
    }

    public BranchNode() {
        super(null);
    }

    /**
     * Adds a test and subtree to this node.
     *
     * @param op
     *            Test operation
     * @param feature
     *            Feature to test
     * @param value
     *            Test or threshold.
     * @param priority
     *            Priority of this test.
     * @param node
     *            Associated TreeNode on passing of test.
     */

    public void addCondition(Operation op, String feature, double value, int priority, TreeNode node) {

        this._conditions.add(new Condition(op, feature, value, priority, node));

        Collections.sort(this._conditions, new Comparator<Condition>() {
            public int compare(Condition one, Condition two) {
                if (one._priority > two._priority)
                    return -1;
                else if (one._priority < two._priority)
                    return 1;

                return one._operation.compareTo(two._operation);
            }
        });
    }

    /**
     * Returns the prediction generated bt this node's descendants that map to
     * the values in features.
     *
     */

    public Map<String, Object> fetchPrediction(Map<String, Double> features) throws TreeNode.TreeNodeException {

        for (Condition condition : this._conditions) {
            //Log.e("inf", condition+"");
            if (condition.evaluate(features)) {
                // Test passed - recurse down the test's associated node and
                // continue...

                return condition.getNode().fetchPrediction(features);
            }
        }

        throw new TreeNode.TreeNodeException(
                "No matching condition for this set of features. Add a DEFAULT condition perhaps?");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode#toString
     * (int)
     */

    public String toString(int indent) throws TreeNodeException {
        StringBuffer sb = new StringBuffer();

        String newline = System.getProperty("line.separator");

        for (Condition condition : this._conditions) {
            if (sb.length() > 0)
                sb.append(newline);

            for (int i = 0; i < indent; i++)
                sb.append("  ");

            sb.append(condition.toString());
            sb.append(newline);

            sb.append(condition.getNode().toString(indent + 1));
        }

        return sb.toString();
    }

    public void addDefaultCondition(TreeNode node) {
        this._conditions.add(new Condition(Operation.DEFAULT, "foo", 0, Condition.LOWEST_PRIORITY, node));

        Collections.sort(this._conditions, new Comparator<Condition>() {
            public int compare(Condition one, Condition two) {
                if (one._priority > two._priority)
                    return -1;
                else if (one._priority < two._priority)
                    return 1;

                return one._operation.compareTo(two._operation);
            }
        });
    }


}
