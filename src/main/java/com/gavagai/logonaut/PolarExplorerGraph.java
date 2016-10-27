package com.gavagai.logonaut;

//import quicktime.std.movies.media.UserData;
//import com.gavagai.logonaut.PolarExplorer.DistanceFunction;

public class PolarExplorerGraph {

/**
	private void setUpView(Graph graph) {

		//Create a simple layout frame
		//specify the Fruchterman-Rheingold layout algorithm
		final SubLayoutDecorator layout = new SubLayoutDecorator(new FRLayout(graph));
		final PickedState ps = new MultiPickedState();
		PluggableRenderer pr = new PluggableRenderer();
		pr.setVertexPaintFunction(new VertexPaintFunction() {
			public Paint getFillPaint(Vertex v) {
				Color k = (Color) v.getUserDatum(DEMOKEY);
				if (k != null)
					return k;
				return Color.white;
			}

			public Paint getDrawPaint(Vertex v) {
				if(ps.isPicked(v)) {
					return Color.cyan;
				} else {
					return Color.BLACK;
				}
			}
		});

		pr.setEdgePaintFunction(new EdgePaintFunction() {
			public Paint getDrawPaint(Edge e) {
				Color k = (Color) e.getUserDatum(DEMOKEY);
				if (k != null)
					return k;
				return Color.blue;
			}
			public Paint getFillPaint(Edge e)
			{
				return null;
			}
		});

		pr.setEdgeStrokeFunction(new EdgeStrokeFunction()
		{
			protected final Stroke THIN = new BasicStroke(1);
			protected final Stroke THICK= new BasicStroke(2);
			public Stroke getStroke(Edge e)
			{
				Color c = (Color)e.getUserDatum(DEMOKEY);
				if (c == Color.LIGHT_GRAY)
					return THIN;
				else 
					return THICK;
			}
		});


		final VisualizationViewer vv = new VisualizationViewer(layout, pr);
		vv.setBackground( Color.white );
		//Tell the renderer to use our own customized color rendering

		//add restart button
		JButton scramble = new JButton("Restart");
		scramble.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				vv.repaint(); //restart?
			}

		});

		DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
		vv.setGraphMouse(gm);
		vv.setPickSupport(new ShapePickSupport(vv)); // added vv to argument
		vv.setPickedVertexState(ps); // chg to ..Vertex..

		final JToggleButton groupVertices = new JToggleButton("Group Clusters");

		//Create slider to adjust the number of edges to remove when clustering
		final JSlider edgeBetweennessSlider = new JSlider(JSlider.HORIZONTAL);
		edgeBetweennessSlider.setBackground(Color.WHITE);
		edgeBetweennessSlider.setPreferredSize(new Dimension(210, 50));
		edgeBetweennessSlider.setPaintTicks(true);
		edgeBetweennessSlider.setMaximum(graph.getEdges().size());
		edgeBetweennessSlider.setMinimum(0);
		edgeBetweennessSlider.setValue(0);
		edgeBetweennessSlider.setMajorTickSpacing(10);
		edgeBetweennessSlider.setPaintLabels(true);
		edgeBetweennessSlider.setPaintTicks(true);

		final JPanel eastControls = new JPanel();
		eastControls.setOpaque(true);
		eastControls.setLayout((LayoutManager) new BoxLayout(eastControls, BoxLayout.Y_AXIS));
		eastControls.add(Box.createVerticalGlue());
		eastControls.add(edgeBetweennessSlider);

		final String COMMANDSTRING = "Edges removed for clusters: ";
		final String eastSize = COMMANDSTRING + edgeBetweennessSlider.getValue();

		final TitledBorder sliderBorder = BorderFactory.createTitledBorder(eastSize);
		eastControls.setBorder(sliderBorder);
		//eastControls.add(eastSize);
		eastControls.add(Box.createVerticalGlue());

		groupVertices.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				clusterAndRecolor(layout, edgeBetweennessSlider.getValue(), 
						similarColors, e.getStateChange() == ItemEvent.SELECTED);
			}});


		clusterAndRecolor(layout, 0, similarColors, groupVertices.isSelected());

		edgeBetweennessSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int numEdgesToRemove = source.getValue();
					clusterAndRecolor(layout, numEdgesToRemove, similarColors,
							groupVertices.isSelected());
					sliderBorder.setTitle(
							COMMANDSTRING + edgeBetweennessSlider.getValue());
					eastControls.repaint();
					vv.validate();
					vv.repaint();
				}
			}
		});

		Container content = getContentPane();
		content.add(new GraphZoomScrollPane(vv));
		JPanel south = new JPanel();
		JPanel grid = new JPanel(new GridLayout(2,1));
		grid.add(scramble);
		grid.add(groupVertices);
		south.add(grid);
		south.add(eastControls);
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createTitledBorder("Mouse Mode"));
		p.add(gm.getModeComboBox());
		south.add(p);
		content.add(south, BorderLayout.SOUTH);
	}

	public void clusterAndRecolor(SubLayoutDecorator layout,
			int numEdgesToRemove,
			Color[] colors, boolean groupClusters) {
		//Now cluster the vertices by removing the top 50 edges with highest betweenness
		//		if (numEdgesToRemove == 0) {
		//			colorCluster( g.getVertices(), colors[0] );
		//		} else {

		Graph g = layout.getGraph();
		layout.removeAllSubLayouts();

		EdgeBetweennessClusterer clusterer =
			new EdgeBetweennessClusterer(numEdgesToRemove);
		Set clusterSet = clusterer.transform(g);
		// ClusterSet clusterSet = clusterer.extract(g);
		List edges = clusterer.getEdgesRemoved();

		int i = 0;
		//Set the colors of each node so that each cluster's vertices have the same color
		for (Iterator cIt = clusterSet.iterator(); cIt.hasNext();) {

			Set vertices = (Set) cIt.next();
			Color c = colors[i % colors.length];

			colorCluster(vertices, c);
			if(groupClusters == true) {
				groupCluster(layout, vertices);
			}
			i++;
		}
		for (Iterator it = g.getEdges().iterator(); it.hasNext();) {
			Edge e = (Edge) it.next();
			if (edges.contains(e)) {
				e.setUserDatum(DEMOKEY, Color.LIGHT_GRAY, UserData.REMOVE);
			} else {
				e.setUserDatum(DEMOKEY, Color.BLACK, UserData.REMOVE);
			}
		}

	}

	private void colorCluster(Set vertices, Color c) {
		for (Iterator iter = vertices.iterator(); iter.hasNext();) {
			Vertex v = (Vertex) iter.next();
			v.setUserDatum(DEMOKEY, c, UserData.REMOVE);
		}
	}

	private void groupCluster(SubLayoutDecorator layout, Set vertices) {
		if(vertices.size() < layout.getGraph().numVertices()) {
			Point2D center = layout.getLocation((ArchetypeVertex)vertices.iterator().next());
			SubLayout subLayout = new CircularSubLayout(vertices, 20, center);
			layout.addSubLayout(subLayout);
		}
	}
	
	public UndirectedSparseGraph<String,Edge> getGraph() {
		return graph;
	}
	public void setGraph(UndirectedSparseGraph<String,Edge> graph) {
		this.graph = graph;
	}
	private static final Object DEMOKEY = "DEMOKEY";

	public final Color[] similarColors =
	{
			new Color(216, 134, 134),
			new Color(135, 137, 211),
			new Color(134, 206, 189),
			new Color(206, 176, 134),
			new Color(194, 204, 134),
			new Color(145, 214, 134),
			new Color(133, 178, 209),
			new Color(103, 148, 255),
			new Color(60, 220, 220),
			new Color(30, 250, 100)
	};

	public class DistanceFunction implements Transformer<PolarExplorer.Edge,Integer> {
		int constant = 100;
		public DistanceFunction(int c) { this.constant = c;}
		public Integer transform(PolarExplorer.Edge e) {
			//System.out.println("boo "+e+" "+e.weight+" "+this.constant+" "+this.constant*e.weight+" "+Math.round(this.constant - this.constant*e.weight));
			e.setLength((int) Math.round(constant - constant*e.weight));
			return e.getLength();
		}
	}
	public void coherence(Vector<String> candidateWords) {
		Hashtable<String,float[]> v = new Hashtable<String,float[]>();
		GetVectorsForWord vectorGetter = new GetVectorsForWord(properties);
		Vector<String> words = new Vector<String>();
		for (String w: candidateWords) {
			try {
				float[] fv = vectorGetter.getContextVector(w);
				if (fv != null) {v.put(w,fv); words.add(w); }			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		int i = 0;
		int j = 0;
		double max = 0d;
		double min = 1d;
		String minstr = "";
		String maxstr = "";
		double[][] corr = new double[words.size()][words.size()];
		String[] windex = new String[words.size()];
		float averageDistanceFromEachOther = 0.0f;
		int ii = 0;
		for (String w1: words) {
			windex[i] = w1;
			j = 0;
			for (String w2: words) {
				if (! w1.equals(w2)) {
					//					int i = 0; for (float ff: v.get(w1)) {if (ff>0) {System.out.print(i+":"+ff+" ");} i++;}System.out.println();
					//					int j = 0; for (float ff: v.get(w2)) {if (ff>0) {System.out.print(j+":"+ff+" ");} j++;}System.out.println();
					double l = VectorMath.cosineSimilarity(v.get(w1),v.get(w2));
					if (l > 0.1f) System.out.println(i+" "+j+" "+w1+" "+w2+" "+l); // Math.round(100000*VectorMath.cosineSimilarity(v.get(w1),v.get(w2)))/100000d);	
					corr[i][j] = l;
					if (l < min) {min = l; minstr = w1+"<->"+w2;}
					if (l > max) {max = l; maxstr = w1+"<->"+w2;}
					averageDistanceFromEachOther += l; ii++;
				}
				j++;
			}		
			i++;
		}
		if (ii > 0) {		System.out.println(averageDistanceFromEachOther/ii);}
		System.out.println(minstr +" " +min);
		System.out.println(maxstr + " "+ max);

		UndirectedSparseGraph<String,Edge> g = new UndirectedSparseGraph<String,Edge>();
		for (int iii = 0; iii < windex.length; iii++) {
			g.addVertex(windex[iii]);
			for (int jjj=0; jjj < iii; jjj++) {
				if (corr[iii][jjj] > 0.3)	g.addEdge(new Edge(windex[iii],windex[jjj],corr[iii][jjj]),windex[iii], windex[jjj]);
			}
		}
		setGraph(g);


		float[] centroid = VectorMath.centroid(v.values());
		g.addVertex("centroid");
		if (words.size() > 0) {
			float averageDistanceFromCentroid = 0.0f;
			for (String w: words) {
				double l = VectorMath.cosineSimilarity(v.get(w),centroid);
				System.out.println(w+" "+l);
				averageDistanceFromCentroid += l;
				//if (l > 0.3) 
				g.addEdge(new Edge("centroid",w,l),w,"centroid");
			}
			System.out.println(averageDistanceFromCentroid/words.size());
		}	
	}

	
	public static void main(String[] args) {
		final Properties esProperties = new Properties();
		esProperties.setProperty("test", "association");
		esProperties.setProperty("wordspace", "3");
		esProperties.setProperty("userid","monitor");
		esProperties.setProperty("password","monitor");
		esProperties.setProperty("host","stage-core1.gavagai.se");
		esProperties.setProperty("ear","stage-core1");
		
  	     //	cd.start();
		// Add a restart button so the graph can be redrawn to fit the size of the frame
//		JFrame jf = new JFrame();
//		jf.getContentPane().add(cd);
//
//		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		jf.pack();
//		jf.setVisible(true);
		 
		String[] wds = // {"anfall", "anfallen", "anfaller", "anfallet", "anföll", "attack", "attacker", "attackera", "attackerad", "attackerade", "attackerades", "attackerar", "attackeras", "attackerat", "avrätta", "avrättad", "avrättar", "avrättas", "avrättat", "bomb", "bomba", "bombad", "bombar", "bombat", "bombats", "brinn", "brinna", "bränd", "bränn", "bränner", "däng", "dö", "död", "döda", "dödar", "dödas", "explodera", "exploderade", "exploderar", "gisslan", "handgripligheter", "helvete", "hot", "hota", "hotade", "hotar", "hotas", "hugga", "hugger", "hänga", "hänger", "hängs", "ihjäl", "illa", "knivskuren", "knivskära", "kravall", "kravaller", "kravallerna", "krig", "kriga", "krigande", "krigar", "kriget", "käften", "körd", "kört", "misshandel", "misshandla", "misshandlade", "misshandlar", "misshandlas", "missil", "missilen", "missiler", "missilerna", "mord", "mordet", "mörda", "mördar", "mördas", "pisk", "piska", "piskar", "piskas", "pyrt", "raket", "rakter", "rakterna", "risigt", "rådäng", "råpisk", "skada", "skadad", "skadade", "skadar", "skadas", "skadats", "skjut", "skjuta", "skjutande", "skjuten", "skjuter", "skjuts", "skuren", "skära", "slagen", "slut", "slå", "slår", "smälla", "smäller", "sparka", "sparkar", "sparkas", "spräng", "spränga", "sprängd", "spränger", "sprängmedel", "sprängs", "sprängt", "spöstraff", "stack", "stick", "sticker", "straffa", "straffad", "straffar", "straffas", "stryk", "strypa", "stryper", "stryps", "strypt", "stucken", "terror", "terrorisera", "terroriserande", "terroriserar", "terroriserat", "terrorism", "terrorist", "terrorister", "terroristerna", "terroristernas", "tortera", "torterad", "torterade", "torterat", "tortyr", "utrota", "utrotade", "utrotades", "utrotar", "utrotas", "våld", "våldet", "våldsamheter", "våldsamheterna", "våldsamma", "våldsamt"};
			//	{"moderaterna", "grön", "socialdemokrat","socialdemokraterna","folkpartiet","miljöpartiet","sverige","riksdagen","svpol","centerpartiet","centern","liberal","liberalerna","vänstern","vänsterpartiet","höger","vänster","rasist","kristdemokraterna","sverigedemokraterna","valet","piratpartiet","konservativ","nationalist","sverige","socialist","nazist"};
		{"a","b","c","d","e","f"};
		PolarExplorer pe = new PolarExplorer(esProperties);
		
		UndirectedSparseGraph<String,Edge> g = new UndirectedSparseGraph<String,Edge>();
		g.addVertex("centroid");
		for (int iii = 0; iii < wds.length; iii++) {
			g.addVertex(wds[iii]);
			for (int jjj=0; jjj < iii; jjj++) {
				g.addEdge(pe.new Edge(wds[iii],wds[jjj],iii*0.1),wds[iii], wds[jjj]);
			}
			g.addEdge(pe.new Edge("centroid",wds[iii],0.1),"centroid",wds[iii]);	
		}
		SpringLayout<String,Edge> sl = new SpringLayout<String,Edge>(g,pe.new DistanceFunction(100));
		VisualizationViewer<String,Edge> vv = new VisualizationViewer<String,Edge>(sl);
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vv.setVertexToolTipTransformer(new ToStringLabeller());
		vv.setEdgeToolTipTransformer(new ToStringLabeller());
		JFrame jf = new JFrame();
		jf.getContentPane().add(vv);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.pack();
		jf.setVisible(true);
		sl.setRepulsionRange(100);
		sl.setForceMultiplier(0.1d);

	}
**/
}
