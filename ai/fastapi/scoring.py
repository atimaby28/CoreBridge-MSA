from typing import Sequence


def rule_score(required_skills: list[str], candidate_skills: list[str], cosine_sim: float) -> dict:
    req = [s.lower() for s in required_skills]
    cand = [s.lower() for s in candidate_skills]

    if not required_skills:
        skill_ratio = 0.0
    else:
        skill_ratio = len(set(req) & set(cand)) / max(1, len(set(req)))

    skill_score = skill_ratio * 40.0
    similarity_score = max(0.0, min(1.0, cosine_sim)) * 40.0

    bonus = 0.0
    if "kafka" in cand:
        bonus += 5.0
    if "kubernetes" in cand:
        bonus += 5.0
    if "docker" in cand:
        bonus += 3.0
    if "aws" in cand or "gcp" in cand or "azure" in cand:
        bonus += 2.0
    bonus = min(bonus, 20.0)

    total = max(0.0, min(100.0, skill_score + similarity_score + bonus))

    # 등급 산정
    if total >= 85:
        grade = "A"
    elif total >= 70:
        grade = "B"
    elif total >= 55:
        grade = "C"
    elif total >= 40:
        grade = "D"
    else:
        grade = "F"

    return {
        "skill_ratio": round(skill_ratio, 3),
        "skill_score": round(skill_score, 1),
        "similarity_score": round(similarity_score, 1),
        "bonus_score": round(bonus, 1),
        "total_score": round(total, 1),
        "grade": grade,
    }
