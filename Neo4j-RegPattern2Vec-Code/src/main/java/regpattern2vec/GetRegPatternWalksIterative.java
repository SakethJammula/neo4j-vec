/*
Author: Saketh Reddy Jammula
Code: A neo4j implementation plugin for RegPattern2Vec algorithm
*/
package regpattern2vec;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetRegPatternWalksIterative {
    @Context
    public Log log;

    @Context
    public Transaction transaction;

    @Procedure(value = "regpattern2vec.GetRegPatternWalksIterative")
    @Description("Generates random walks based on regular pattern iteratively.")

    public Stream<Walks> getRegPatternWalks(
            @Name("pattern") String pattern, // pattern = (Author)(Article)*(Venue)
            @Name("walks") Long walks, // walks = 10
            @Name("length") Long walkLength // walkLength = 10
    ) {
        Long walksNumber = walks;
        Long walkLengthInt = walkLength;
        int nextStateToGo = -1; // Variable that decides the next state to traverse to.

        String dfaUsed = "(q0,Author->q1),(q1,Article->q1|Venue->q2)";
        String dfaUsedRev = "(q0,Venue->q1),(q1,Article->q1|Author->q2)";


        Walks wks = new Walks();
        wks.walks = new ArrayList<>();

        // Initially used nested map to populate the transition table
        Map<String, Map<String, String>> transitionFunc = computeTransitionFunction(dfaUsed);
        Map<String, Map<String, String>> transitionFuncRev = computeTransitionFunction(dfaUsedRev);

        // Use the nested map transitionFunc and convert it to two-dimensional array
        // This two-dimensional array will be used to steer random walks, not the nested map
        // Also, store the input symbols(Author, Article, Venue) as (0,1,2) and the states
        // (q0,q1,q2) as (0,1,2) as well for the transition table

        // Extract q0, q1, and q2 from the transition function as a set of states

        // Write a function to return states as a set of strings and takes transitionFunc as input
        Set<String> states = states(transitionFunc);
        Set<String> statesRev = states(transitionFuncRev);



        // Create maps to store the state names and their integer values

        // Write a function that takes states as input and returns stateToInt

        Map<String, Integer> stateToInt = stateToInt(states);
        Map<String, Integer> stateToIntRev = stateToInt(statesRev);


        // Write a function that takes states as input and returns intToState

        Map<Integer, String> intToState = intToState(stateToInt, states);
        Map<Integer, String> intToStateRev = intToState(stateToIntRev, statesRev);


        // Write a function that takes transitionFunc as input and returns symbolToInt

        Map<String, Integer> symbolToInt = symbolToInt(transitionFunc);
        Map<String, Integer> symbolToIntRev = symbolToInt(transitionFuncRev);

        // Write a function that takes transitionFunc as input and returns intToSymbol

        Map<Integer, String> intToSymbol = intToSymbol(transitionFunc);
        Map<Integer, String> intToSymbolRev = intToSymbol(transitionFuncRev);


        // Create a transition table based on state and input symbols as two-dimensional array

        // Write a function that takes stateToInt, SymbolToInt, transitionFunc and return a transition table

        int[][] transitionTable = transitionTable(stateToInt, symbolToInt, transitionFunc);
        int[][] transitionTableRev = transitionTable(stateToIntRev, symbolToIntRev, transitionFuncRev);

        // Log transitionTable content
        int numRows = transitionTable.length;
        int numCols = transitionTable[0].length;

        // Print the contents of the transition table
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
            }
        }

        // Log transitionTable content
        int numRowsRev = transitionTableRev.length;
        int numColsRev = transitionTableRev[0].length;

        // Print the contents of the transition table
        for (int i = 0; i < numRowsRev; i++) {
            for (int j = 0; j < numColsRev; j++) {
            }
        }

        // For now, I am assuming, state q0 has only one valid state and proceeding further.
        // state 0
        // transitiontable[0][0] is the only valid state
        String author = "";
        for(int j = 0; j<transitionTable[0].length; j++){
            // check valid states
            if(transitionTable[0][j] != 0){
                if(j == 0){
                    author = intToSymbol.get(j);
                    nextStateToGo = transitionTable[0][0]; // 1
                }
                if(j == 1){
                    String conference = intToSymbol.get(j);
                    nextStateToGo = transitionTable[0][1];
                }
            }
        }
        int finalNextStateToGo = nextStateToGo;

        // Based on the valid state, choose all the nodes of the node type. In this case, Author
        // Call the getWalkIterative method. This method returns the generated walks for every iteration of walksNumber for
        // every startNode (In this case Author_0, Author_1089,......

        transaction.findNodes(Label.label(author))
                .forEachRemaining(startNode -> {
                    for(int i = 0; i < walksNumber; i++){
                        wks.walks.add(getWalkIterative(startNode, walkLengthInt, transitionTable, finalNextStateToGo, intToSymbol, transitionTableRev, intToSymbolRev));
                    }
                });
        Stream<Walks> saveWalks = Stream.of(wks);
        saveWalks(saveWalks, "walks.txt");
        return Stream.of(wks);
    }


    public Map<String, Map<String, String>> computeTransitionFunction(String dfaUsed) {
        Map<String, Map<String, String>> transitionFunc = new LinkedHashMap<>();
        String[] transitions = dfaUsed.split("\\),\\(");
        for (String transition : transitions) {
            transition = transition.replaceAll("\\(|\\)", "");
            String[] parts = transition.split(",");
            String state = parts[0];
            String[] inputs = parts[1].split("\\|");
            for (String input : inputs) {
                String[] pair = input.split("->");
                String symbol = pair[0];
                String next = pair[1];
                if (!transitionFunc.containsKey(state)) {
                    transitionFunc.put(state, new LinkedHashMap<>());
                }
                transitionFunc.get(state).put(symbol, next);
            }
        }
        //  transitionFunc: {q0={Author=q1}, q1={Venue=q2, Article=q1}}
        return transitionFunc;
    }
    private Set<String> states(Map<String, Map<String, String>> transitionFunc) {
        Set<String> states = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, String>> entry : transitionFunc.entrySet()) {
            String state = entry.getKey();
            states.add(state);
            Map<String, String> transitions = entry.getValue();
            for (String transitionState : transitions.values()) {
                states.add(transitionState);
            }
        }
        return states;
    }
    private Map<String, Integer> stateToInt(Set<String> states) {
        Map<String, Integer> stateToInt = new LinkedHashMap<>();
        int nextStateInt = 0;
        for (String state : states) {
            stateToInt.put(state, nextStateInt);
            nextStateInt++;
        }
        return stateToInt;
    }
    private Map<String, Integer> symbolToInt(Map<String, Map<String, String>> transitionFunc) {
        Map<String, Integer> symbolToInt = new LinkedHashMap<>();
        int nextSymbolInt = 0;
        for (Map<String, String> transitions : transitionFunc.values()) {
            for (String symbol : transitions.keySet()) {
                if (!symbolToInt.containsKey(symbol)) {
                    symbolToInt.put(symbol, nextSymbolInt);
                    nextSymbolInt++;
                }
            }
        }
        return symbolToInt;
    }
    private Map<Integer, String> intToState(Map<String, Integer> stateToInt, Set<String> states) {
        Map<Integer, String> intToState = new LinkedHashMap<>();
        int nextIntState = 0;
        for (String state : states) {
            stateToInt.put(state, nextIntState);
            intToState.put(nextIntState, state);
            nextIntState++;
        }
        return intToState;
    }
    private Map<Integer, String> intToSymbol(Map<String, Map<String, String>> transitionFunc) {
        Map<String, Integer> symbolToInt = new LinkedHashMap<>();
        Map<Integer, String> intToSymbol = new LinkedHashMap<>();
        int nextSymbolInt = 0;
        for (Map<String, String> transitions : transitionFunc.values()) {
            for (String symbol : transitions.keySet()) {
                if (!symbolToInt.containsKey(symbol)) {
                    symbolToInt.put(symbol, nextSymbolInt);
                    intToSymbol.put(nextSymbolInt, symbol);
                    nextSymbolInt++;
                }
            }
        }
        return intToSymbol;
    }
    private int[][] transitionTable(Map<String, Integer> stateToInt, Map<String, Integer> symbolToInt, Map<String, Map<String, String>> transitionFunc) {
        int numStates = stateToInt.size();
        int numSymbols = symbolToInt.size();

        int[][] transitionTable = new int[numStates][numSymbols];

        // Populate transitionTable
        for (String state : transitionFunc.keySet()) {
            Map<String, String> transitions = transitionFunc.get(state);
            int stateInt = stateToInt.get(state);
            for (String symbol : transitions.keySet()) {
                String nextState = transitions.get(symbol);
                int symbolInt = symbolToInt.get(symbol);
                int nextStateIntVal = stateToInt.get(nextState);
                transitionTable[stateInt][symbolInt] = nextStateIntVal;
            }
        }
        return transitionTable;
    }
    private String getWalkIterative(Node startNode, Long walkLengthInt, int[][] transitionTable, int nextStateToGo, Map<Integer, String> intToSymbol, int[][] transitionTableRev, Map<Integer,String> intToSymbolRev) {
        // Using Transition class with row, col, value and nodeType to capture the current cell information
        // on the transition table
        class Transition {
            int row;
            int col;
            int value;
            String nodeType;

            public Transition(int row, int col, int value, String nodeType) {
                this.row = row;
                this.col = col;
                this.value = value;
                this.nodeType = nodeType;
            }
        }

        // Author_1019Article_4Venue_1001Article_26208Venue_1001Article_133957Author_10877Article_57065Author_10876Article_57065

        String firstaddVal = startNode.getElementId();
       String firstuseVal = firstaddVal.substring(firstaddVal.lastIndexOf(":") + 1);
        String propertyVal = "a" + firstuseVal + " "; // Author_1019
        boolean forw = true;
        // Loop until the walkLengthInt
        for (int i = 1; i < walkLengthInt; i++) {
            // walkLengthInt = 10;
            ArrayList<String> validNodeTypes = new ArrayList<>(); // to store valid nodetypes
            ArrayList<Transition> validTransitions = new ArrayList<>(); // to store valid transitions cell related values

            if(forw){
                // walk forward
                // Based on nextStateToGo, choose the valid node types of that row
                for (int j = 0; j < transitionTable[0].length; j++) {
                    // check valid states
                    int value = transitionTable[nextStateToGo][j]; // 1,2
                    if (value != 0) {
                        String nodeType = intToSymbol.get(j); // Article, Venue
                        validNodeTypes.add(nodeType); // Append all the valid node types here
                        validTransitions.add(new Transition(nextStateToGo, j, value, nodeType));
                    }
                }
            }
            else{
                // walk backward
                // Based on nextStateToGo, choose the valid node types of that row
                for (int j = 0; j < transitionTableRev[0].length; j++) {
                    // check valid states
                    int value = transitionTableRev[nextStateToGo][j]; // 0 1 2
                    if (value != 0) {
                        String nodeType = intToSymbolRev.get(j); // Article, Author
                        validNodeTypes.add(nodeType); // Append all the valid node types here
                        validTransitions.add(new Transition(nextStateToGo, j, value, nodeType));
                    }
                }
            }


            // Based on validNodeTypes collected and the current node we have, find the neighbors
            // of the current node that have neighbors whose node types are of valid node types.
            Iterator<Node> pickedNodes = neighborNode(startNode, validNodeTypes); // (Article_26208, (Article, Author)
            //pickedNodes returns a node iterator with all the valid nodes of the current node.
            // Pick a random node among this pickedNodes

            Node pickedNode = null;
            // Author_1019Article_4Venue_1001Article_26208Venue_1001Article_133957Author_10877Article_57065Author_10876Article_57065
            if(forw){
                pickedNode = BiasedPick(pickedNodes); // (Article and Author nodes, 0->A 1->Ar 2->V)
            }
            else {
                pickedNode = BiasedPick(pickedNodes);
            }
            // pickedNode = Article_26208

            String currLabel = pickedNode.getLabels().iterator().next().name(); // Article
            // Append the walk with the pickedNode to the propertyVal (which is our walk)
            // Check for node type
            // If node type == venue => append v
            String Venue = "Venue";
            String Author = "Author";
            String Article = "Article";


            String addVal = "";
            String useVal = "";
            if(currLabel.equals(Venue)){
                addVal = pickedNode.getElementId();
                useVal = addVal.substring(addVal.lastIndexOf(":") + 1);
                propertyVal +=  "v" + useVal + " ";
            }
            else if(currLabel.equals(Author)){
                addVal = pickedNode.getElementId();
                useVal = addVal.substring(addVal.lastIndexOf(":") + 1);
                propertyVal += "a" + useVal + " ";
            }
            else if(currLabel.equals(Article)){
                addVal = pickedNode.getElementId();
                useVal = addVal.substring(addVal.lastIndexOf(":") + 1);
                propertyVal += "i" + useVal + " ";
            }
            // If node type == article => append i
            // if node type == author => append a

            // Author_1019Article_4Venue_0Article_26208
            // To continue the iteration, assign the pickedNode as the startNode(current node)
            startNode = pickedNode; // Article_26208

            // Get its label, based on the label, find the cell information so as to find out what
            // is the nextStateToGo

            int targetRow = -1;
            int targetCol = -1;
            int targetValue = -1;

            for (Transition t : validTransitions) {
                if (t.nodeType.equals(currLabel)) {
                    targetRow = t.row; // state value
                    targetCol = t.col; // symbol value
                    targetValue = t.value; // element value. This is our next state to go  // 2
                    break;
                }
            }

            if(forw){
                if(targetValue != transitionTable.length - 1){ // 2 != 2 true
                    // This means the targetValue is not the final state, so we can traverse to the targetValue state
                    forw = true;
                    nextStateToGo = targetValue; // 1
                }
                else{
                    // In final state, there will be no valid transitions.
                    // But we have to still continue the walk as the walk length is not reached yet.
                    // regpattern2vec generates random walks backwards. Need to understand this further
                    // to populate the complete walk until the walklength
                    forw = false;
                    nextStateToGo = 1; // 0? 1? 2?
                }
            }
            else{
                if(targetValue != transitionTableRev.length - 1){ // 1 != 2
                    // This means the targetValue is not the final state, so we can traverse to the targetValue state
                    forw = false;
                    nextStateToGo = targetValue; // 1
                }
                else{
                    // In final state, there will be no valid transitions.
                    // But we have to still continue the walk as the walk length is not reached yet.
                    // regpattern2vec generates random walks backwards. Need to understand this further
                    // to populate the complete walk until the walklength
                    forw = true;
                    nextStateToGo = 1;
                }
            }
        }
        log.info(propertyVal);
        return propertyVal;
    }
    private Node BiasedPick(Iterator<Node> pickedNodes) {
        Node pickedNode = null;
        Random random = new Random();

        List<Node> nodeList = new ArrayList<>();

        // Convert Iterator to List

        while (pickedNodes.hasNext()){
            nodeList.add(pickedNodes.next());
        }
        double totalWeight = 0.0;

        // Calculate the total wight for normalization

        for(Node node : nodeList){
            int degree = node.getDegree();
            totalWeight += 1.0/degree;
        }

        // Generate a random number between 0 and the total weight

        double randomNumber = random.nextDouble() * totalWeight;

        // Select the next node based on the random number and weights

        double cumulativeWeight = 0.0;

        // int count = 0;

        for (Node node : nodeList) {
            int degree = node.getDegree();
            double weight = 1.0 / degree;

            cumulativeWeight += weight;
            if (randomNumber <= cumulativeWeight) {
                return node;
            }
        }
        return null;
    }
    private Iterator<Node> neighborNode(Node startNode, ArrayList<String> validNodeTypes) {
        List<Node> candidateNodes = new ArrayList<>();
        for (Relationship rel : startNode.getRelationships()) {
            Node otherNode = rel.getOtherNode(startNode);
            if (validNodeTypes.contains(otherNode.getLabels().iterator().next().name())) {
                candidateNodes.add(otherNode);
            }
        }
        return candidateNodes.iterator();
    }
    // The generated walks are stored in a txt file
    public static void saveWalks(Stream<Walks> wks, String filePath) {

        try (FileWriter writer = new FileWriter(filePath)) {
            List<String> allWalks = wks
                    .flatMap(w -> w.walks.stream())
                    .collect(Collectors.toList());
            for (String walk : allWalks) {
                writer.write(walk + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
