package edu.northwestern.sohrob.activityrecognition.activityrecognition.trees.parsers;

import edu.northwestern.sohrob.activityrecognition.activityrecognition.trees.TreeNode;

/**
 * Created by sohrob on 11/10/14.
 */
public abstract class TreeNodeParser {

    public static class ParserNotFound extends Exception {
        public ParserNotFound(String message) {
            super(message);
        }

        private static final long serialVersionUID = 3671610661977748070L;
    }

    /**
     * Inspects model content and generates the TreeNode (and descendants)
     * corresponding to the model's content.
     *
     * @param content
     *            String representation of the decision tree.
     *
     * @return Root TreeNode of the generated decision tree.
     *
     * @throws ParserNotFound
     *             Thrown on parser error.
     *
     * @throws TreeNode.TreeNodeException
     *             Thrown on tree errors.
     */

    public static TreeNode parseString(String content) throws ParserNotFound,
            TreeNode.TreeNodeException {
        TreeNodeParser parser = null;

        parser = new MatLabBinaryTreeParser();

        if (parser == null)
            throw new TreeNodeParser.ParserNotFound(
                    "Unable to find parser for content.");

        return parser.parse(content);
    }

    /**
     * Abstract method implemented by specific parsers to generate a decision
     * tree.
     *
     * @param content
     *            String representation of the decision tree.
     * @return Root TreeNode of the generated decision tree.
     * @throws TreeNode.TreeNodeException
     *             Thrown on tree errors.
     */

    public abstract TreeNode parse(String content) throws TreeNode.TreeNodeException;

}
