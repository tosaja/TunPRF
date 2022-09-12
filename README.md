# TunPRF
Language identifier using product of relative frequencies and language model adaptation for ITDI 2022 shared task

This software is published just to provide the precise description of how the classification method with the product of relative frequencies and adaptive language models were implemented.

The first argument to the program includes the training texts (one per line) in the VarDial format, e.g. the language code followed by tab and the text in that language. The second argument is a test file without language codes. By quoting out and in various parts of the code a development file formatted similarly to the test file can be evaluated.

In order to compile and run the software, you need the guava library (23.0 was used here, but other versions should work too).
