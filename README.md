# mateplus

This repository contains code for an extended version of the [mate-tools][1] semantic role labeler. Some extensions are still under development, others are described in scientific publications (cf. [Roth and Woodsend, 2014][2]). 

June 2015: The current version achieves state-of-the-art performance on the CoNLL-2009 data set (**87.35** F1-score). It is the best performing system for SRL in English.  

# Dependencies

The following libraries and model files need to be downloaded in order to run _mateplus_:

 * Bernd Bohnet's dependency parser (anna-3.3.jar) and model files (CoNLL2009-ST-English*.model)
 * The WSJ tokenizer from Stanford CoreNLP (stanford-corenlp-3.x.jar) 
 * A recent Java port of LIBLINEAR
 
# References

[1]: http://code.google.com/p/mate-tools/
[2]: Roth, M. and Woodsend, K. (2014): Composition of word representations improves semantic role labelling. Proceedings of the 2014 Conference on Empirical Methods in Natural Language Processing (EMNLP), Doha, Qatar, October, pp. 407--413.
