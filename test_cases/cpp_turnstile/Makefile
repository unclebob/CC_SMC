all : clean TwoCoinTurnstile.h turnstile_test
	./turnstile_test

turnstile_test : turnstile_test.cc

clean :
	rm -f *.o TwoCoinTurnstile.h turnstile_test

TwoCoinTurnstile.h : twoCoinTurnstile.sm
	java -jar ../../build/jar/smc.jar -l Cpp twoCoinTurnstile.sm


