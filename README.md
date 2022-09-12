# TunPRF
Language identifier using product of relative frequencies and language model adaptation for ITDI 2022 shared task

This software is published just to provide the precise description of how the classification method with the product of relative frequencies and adaptive language models were implemented.

The first argument to the program includes the training texts (one per line) in the VarDial format, e.g. the language code followed by tab and the text in that language. The second argument is a test file without language codes. By quoting out and in various parts of the code a development file formatted similarly to the test file can be evaluated.

In order to compile and run the software, you need the guava library (23.0 was used here, but other versions should work too).

If you use this program in producing scientific publications, please refer to: 
@inproceedings{jauhiainen-etal-2012,
title = "Italian Language and Dialect Identification and Regional French Variety Detection using Adaptive Naive Bayes",
author = "Jauhiainen, Tommi  and
       Jauhiainen, Heidi  and
       Lind{\'e}n, Krister",
booktitle = "Proceedings of the Ninth Workshop on NLP for Similar Languages, Varieties and Dialects",
month = oct,
year = "2022",
address = "Gyeongju, Republic of Korea",
publisher = "International Committee on Computational Linguistics (ICCL)”
}
