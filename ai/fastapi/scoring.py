from typing import Sequence

def jaccard(a: Sequence[str], b: Sequence[str]) -> float:
    A, B = set([x.lower() for x in a]), set([x.lower() for x in b])
    if not A or not B:
        return 0.0
    return len(A & B) / len(A | B)

def rule_score(required_skills: list[str], candidate_skills: list[str], cosine_sim: float) -> dict:
    req = [s.lower() for s in required_skills]
    cand = [s.lower() for s in candidate_skills]

    if not required_skills:
        skill_ratio = 0.0
    else:
        skill_ratio = len(set(req) & set(cand)) / max(1, len(set(req)))

    skill_score = skill_ratio * 60.0
    sim_score = max(0.0, min(1.0, cosine_sim)) * 20.0

    bonus = 0.0
    if "kafka" in cand: bonus += 5.0
    if "kubernetes" in cand: bonus += 5.0
    if not any(db in cand for db in ["mysql","mariadb","postgresql"]): bonus -= 5.0

    total = max(0.0, min(100.0, skill_score + sim_score + bonus))
    return {
        "skill_ratio": round(skill_ratio, 3),
        "skill_score": round(skill_score, 1),
        "sim_score": round(sim_score, 1),
        "bonus": round(bonus, 1),
        "total": round(total, 1),
    }
