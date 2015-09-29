# mateplus

This repository contains code for an extended version of the [mate-tools][1] semantic role labeler. Most extensions are described in [Roth and Woodsend, 2014][2]. Unpublished extensions include feature selection routines and some currently undescribed additional functionalities.

**June 2015**: The current version achieves state-of-the-art performance on the CoNLL-2009 data set. With F1-scores of **87.33** in-domain and **76.38** out-of-domain, it is the best performing system for SRL in English to date. With an in-domain F1-score of **81.38**, it is also the best SRL system available for German. A demo is available online [here](http://homepages.inf.ed.ac.uk/mroth/demo.html).

**September 2015**: This repository now also includes code for our frame-semantic SRL model introduced in [Roth and Lapata, 2015][3]. This model achieves a state-of-the-art F1-score of **76.88** on identifying and labeling arguments in FrameNet 1.5 full texts (using gold frames). Installation instructions are provided below. If you want to try out the frame-semantic SRL model online, please use the demo [here](http://homepages.inf.ed.ac.uk/mroth/demo.html).   

# Dependencies

The following libraries and model files need to be downloaded in order to run _mateplus_ on English text:

 * Bernd Bohnet's dependency parser and model files ([`anna-3.3.jar` and `CoNLL2009-ST-English*.model`](http://code.google.com/p/mate-tools/downloads/))<sup>1</sup>
 * The WSJ tokenizer from Stanford CoreNLP ([`stanford-corenlp-3.x.jar`](http://nlp.stanford.edu/software/corenlp.shtml)) 
 * A recent Java port of LIBLINEAR ([`liblinear-x.jar`](http://liblinear.bwaldvogel.de/))
 * The most recent _mateplus_ SRL model (June 2015), available from Google Drive [here][4] 

In order to run the FrameNet and context extensions of mateplus (i.e., _framat_ and _framat+context_), please also download the following dependencies:

 * FrameNet version 1.5, available from ICSI Berkeley [here][5]
 * SEMAFOR (for frame identification, including MSTparser for preprocessing), available from CMU [here][6]
 * Stanford CoreNLP (for coreference resolution), available from Stanford [here][7]
 * GloVe 1.0a + pre-trained vectors (September 2015), available from Google Drive [here][8]  
 * The most recent _framat_ SRL model (September 2015), available from Google Drive [here][9] 

To run _mateplus_ on German text, additional preprocessing libraries need to be downloaded:

 * Bernd Bohnet's joint parsing model ([`transition-1.30.jar`, `pet-ger-S2a-X` and `lemma-ger-3.6.model`](https://code.google.com/p/mate-tools/wiki/ParserAndModels))
 * OpenNLP tokenizer (libraries from [`apache-opennlp-1.5.3*` and `de-token.bin`](http://www.mirrorservice.org/sites/ftp.apache.org//opennlp/))
 * The most recent _mateplus_ SRL model for German (June 2015), available from Google Drive [here][10]  

If you want to run _mateplus_ on German text using [ParZu](https://github.com/rsennrich/parzu) as an external dependency parser (recommended for non-newswire text), please use [this model][11] from Google Drive.

# Running mateplus  

If copies of all required libraries and models are available in the subdirectories `lib/` and `models/`, respectively, mateplus can simply be executed as a standalone application using the scripts `scripts/parse.sh` and `scripts/parse_framenet.sh`. These scripts run necessary preprocessing tools on a given input text file (assuming one sentence per line), and apply our state-of-the-art model for identifying and role labeling of semantic predicate-argument structures. For German, please use the script `scripts/parse-ger.sh` (recommended for newswire text) or `scripts/parse-ger-ext.sh` (recommended for non-newswire text).

It is also possible to apply the mateplus SRL model on already preprocessed text in the CoNLL 2009 format, using the Java class `se.lth.cs.srl.Parse`. Since mateplus is trained based on preprocessed input from specific pipelines, however, we strongly recommend to use the complete pipeline to achieve best performance. 

# References

[1]: http://code.google.com/p/mate-tools/
[2]: http://www.aclweb.org/anthology/D14-1045.pdf
[3]: https://tacl2013.cs.columbia.edu/ojs/index.php/tacl/article/view/652/147
[4]: http://docs.google.com/uc?id=0B5aLxfs6OvZBUHRFOEcyLTMzWFE&export=download
[5]: https://framenet.icsi.berkeley.edu/fndrupal/framenet_request_data
[6]: http://www.cs.cmu.edu/~ark/SEMAFOR/
[7]: http://nlp.stanford.edu/software/corenlp.shtml
[8]: http://drive.google.com/uc?id=0B5aLxfs6OvZBTFlSa1BUbHh2OWM&export=download
[9]: http://drive.google.com/uc?id=0B5aLxfs6OvZBemZMVnNHT2E1SDg&export=download
[10]: http://drive.google.com/uc?id=0B5aLxfs6OvZBalRtMWIwMkMzWFE&export=download
[11]: http://drive.google.com/uc?id=0B5aLxfs6OvZBTEwyLXpwdTYxVFU&export=download

If you are using mateplus in your work--and we highly recommend you do!--please cite the following publication:

Michael Roth and Kristian Woodsend (2014). Composition of word representations improves semantic role labelling. Proceedings of the 2014 Conference on Empirical Methods in Natural Language Processing (EMNLP), Doha, Qatar, October, pp. 407-413

If you are using the FrameNet based models _Framat_ or _Framat+context_, please cite the following journal paper:

Michael Roth and Mirella Lapata (2015). Context-aware Frame-Semantic Role Labeling. Context-aware frame-semantic role labeling. Transactions of the Association for Computational Linguistics, 3, 449-460.

Depending on which parts of the pipeline you are using, please also cite the following.

***German joint parsing model***: Bernd Bohnet, Joakim Nivre, Igor Boguslavsky, Richárd Farkas, Filip Ginter, Jan Hajic (2013). Joint morphological and syntactic analysis for richly inflected languages. Transactions of the Association for Computational Linguistics (TACL) 1:415--428

***ParZu--The Zurich Dependency Parser***: Rico Sennrich, Martin Volk, Gerold Schneider (2013). Exploiting synergies between open resources for german dependency parsing, POS-tagging, and morphological analysis. In Proceedings of the International Conference on Recent Advances in Natural Language Processing (RANLP), Hissar, Bulgaria. 

***English parsing model***: Bernd Bohnet (2010). Very high accuracy and fast dependency parsing is not a contradiction. The 23rd International Conference on Computational Linguistics (COLING), Beijing, China. 

Original mate-tools ***SRL model***: Anders Björkelund, Love Hafdell, and Pierre Nugues (2009). Multilingual semantic role labeling. In Proceedings of The Thirteenth Conference on Computational Natural Language Learning (CoNLL), Boulder, Colorado, pp. 43--48 



<hr/>
<font size="-1"><sup>1</sup> To reproduce our evaluation results on the CoNLL-2009 data set, preprocessing components must be retrained on the training split only, using 10-fold jackknifing.</font> 
