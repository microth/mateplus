# mateplus

This repository contains code for an extended version of the [mate-tools][1] semantic role labeler. Most extensions are described in [Roth and Woodsend, 2014][2]. Unpublished extensions include feature selection routines and some currently undescribed additional functionalities.

**June 2015**: The current version achieves state-of-the-art performance on the CoNLL-2009 data set. With F1-scores of **87.33** in-domain and **76.38** out-of-domain, It is the best performing system for SRL in English to date. A demo is available online [here](http://homepages.inf.ed.ac.uk/mroth/demo.html).

# Dependencies

The following libraries and model files need to be downloaded in order to run _mateplus_:

 * Bernd Bohnet's dependency parser and model files ([`anna-3.3.jar` and `CoNLL2009-ST-English*.model`](http://code.google.com/p/mate-tools/Downloads/))<sup>1</sup>
 * The WSJ tokenizer from Stanford CoreNLP ([`stanford-corenlp-3.x.jar`](http://nlp.stanford.edu/software/corenlp.shtml)) 
 * A recent Java port of LIBLINEAR ([`liblinear-x.jar`](http://liblinear.bwaldvogel.de/))

The most recent _mateplus_ SRL model (June 2015) can be downloaded from Google Drive [here][3] 

# Running mateplus  

If copies of all required libraries and models are available in the subdirectories `lib/` and `models/`, respectively, mateplus can simply be executed as a standalone application using the script `scripts/parse.sh`. This script runs the mate-tools pipeline to preprocess a given input text file (assuming one sentence per line), and applies our state-of-the-art model for identifying and role labeling of semantic predicate-argument structures.

It is also possible to apply the mateplus SRL model on already preprocessed text in the CoNLL 2009 format, using the Java class `se.lth.cs.srl.Parse`. Since mateplus is trained based on input preprocessed with mate-tools, however, we strongly recommend to use the complete pipeline to achieve best performance. 

# References

[1]: http://code.google.com/p/mate-tools/
[2]: http://www.aclweb.org/anthology/D14-1045.pdf
[3]: http://docs.google.com/uc?id=0B5aLxfs6OvZBUHRFOEcyLTMzWFE&export=download

If you are using mateplus in your work--and we highly recommend you do!--please cite the following publication:

Roth, M. and Woodsend, K. (2014). Composition of word representations improves semantic role labelling. Proceedings of the 2014 Conference on Empirical Methods in Natural Language Processing (EMNLP), Doha, Qatar, October, pp. 407-413


<hr/>
<font size="-1"><sup>1</sup> To reproduce our evaluation results on the CoNLL-2009 data set, preprocessing components must be retrained on the training split only, using 10-fold jackknifing.</font> 
