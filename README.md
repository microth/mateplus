# mateplus

This repository contains code for an extended version of the [mate-tools][1] semantic role labeler. Most extensions are described in [Roth and Woodsend, 2014][2]. Unpublished extensions include feature selection routines and some currently undescribed additional functionalities.

June 2015: The current version achieves state-of-the-art performance on the CoNLL-2009 data set (**87.35** F1-score). It is the best performing system for SRL in English to date. 

# Dependencies

The following libraries and model files need to be downloaded in order to run _mateplus_:

 * Bernd Bohnet's dependency parser and model files (anna-3.3.jar and CoNLL2009-ST-English*.model from http://code.google.com/p/mate-tools/downloads/)
 * The WSJ tokenizer from Stanford CoreNLP (stanford-corenlp-3.x.jar from http://nlp.stanford.edu/software/corenlp.shtml) 
 * A recent Java port of LIBLINEAR (.jar download from http://liblinear.bwaldvogel.de/)

The most recent _mateplus_ SRL model (June 2015) can be downloaded from Google Drive [here][3] 
  
# References

[1]: http://code.google.com/p/mate-tools/
[2]: http://www.aclweb.org/anthology/D14-1045.pdf
[3]: http://docs.google.com/uc?id=0B5aLxfs6OvZBUHRFOEcyLTMzWFE&export=download

If you are using mateplus in your work--and we highly recommend you do!--please cite the following publication:

Roth, M. and Woodsend, K. (2014): Composition of word representations improves semantic role labelling. Proceedings of the 2014 Conference on Empirical Methods in Natural Language Processing (EMNLP), Doha, Qatar, October, pp. 407-413