# mateplus

This repository contains code for an extended version of the [mate-tools][1] semantic role labeler. Most extensions are described in [Roth and Woodsend, 2014][2]. Unpublished extensions include feature selection routines and some currently undescribed additional functionalities.

**June 2015**: The current version achieves state-of-the-art performance on the CoNLL-2009 data set. With F1-scores of **87.33** in-domain and **76.38** out-of-domain, it is the best performing system for SRL in English to date. With an in-domain F1-score of **81.38**, it is also the best SRL system available for German. A demo is available online [here](http://homepages.inf.ed.ac.uk/mroth/demo.html).

# Dependencies

The following libraries and model files need to be downloaded in order to run _mateplus_ on English text:

 * Bernd Bohnet's dependency parser and model files ([`anna-3.3.jar` and `CoNLL2009-ST-English*.model`](http://code.google.com/p/mate-tools/downloads/))<sup>1</sup>
 * The WSJ tokenizer from Stanford CoreNLP ([`stanford-corenlp-3.x.jar`](http://nlp.stanford.edu/software/corenlp.shtml)) 
 * A recent Java port of LIBLINEAR ([`liblinear-x.jar`](http://liblinear.bwaldvogel.de/))
 * The most recent _mateplus_ SRL model (June 2015), available from Google Drive [here][3] 

To run _mateplus_ on German text, additional preprocessing libraries need to be downloaded:

 * Bernd Bohnet's joint parsing model ([`transition-1.30.jar`, `pet-ger-S2a-X` and `lemma-ger-3.6.model`](https://code.google.com/p/mate-tools/wiki/ParserAndModels))
 * OpenNLP tokenizer (libraries from [`apache-opennlp-1.5.3*` and `de-token.bin`](http://www.mirrorservice.org/sites/ftp.apache.org//opennlp/))
 * The most recent _mateplus_ SRL model for German (June 2015), available from Google Drive [here][4]  

# Running mateplus  

If copies of all required libraries and models are available in the subdirectories `lib/` and `models/`, respectively, mateplus can simply be executed as a standalone application using the script `scripts/parse.sh`. This script runs the mate-tools pipeline to preprocess a given input text file (assuming one sentence per line), and applies our state-of-the-art model for identifying and role labeling of semantic predicate-argument structures.

It is also possible to apply the mateplus SRL model on already preprocessed text in the CoNLL 2009 format, using the Java class `se.lth.cs.srl.Parse`. Since mateplus is trained based on input preprocessed with mate-tools, however, we strongly recommend to use the complete pipeline to achieve best performance. 

# Reproducing CoNLL evaluation results

If you want to reproduce our results on the CoNLL-2009 data sets, please make sure to use preprocessing models that are learned on the training splits of CoNLL-2009 only.

# References

[1]: http://code.google.com/p/mate-tools/
[2]: http://www.aclweb.org/anthology/D14-1045.pdf
[3]: http://docs.google.com/uc?id=0B5aLxfs6OvZBUHRFOEcyLTMzWFE&export=download
[4]: http://drive.google.com/uc?id=0B5aLxfs6OvZBalRtMWIwMkMzWFE&export=download

If you are using mateplus in your work--and we highly recommend you do!--please cite the following publication:

Roth, M. and Woodsend, K. (2014). Composition of word representations improves semantic role labelling. Proceedings of the 2014 Conference on Empirical Methods in Natural Language Processing (EMNLP), Doha, Qatar, October, pp. 407-413


<hr/>
<font size="-1"><sup>1</sup> To reproduce our evaluation results on the CoNLL-2009 data set, preprocessing components must be retrained on the training split only, using 10-fold jackknifing.</font> 
