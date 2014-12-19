#ifndef TURNSTILE_H
#define TURNSTILE_H

struct turnstile_actions;
struct turnstile;
struct turnstile *make_turnstile(struct turnstile_actions*);
void turnstile_coin(struct turnstile*);
void turnstile_pass(struct turnstile*);
#endif