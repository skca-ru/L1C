import re

with open('app/src/main/java/l1c/RunYBase.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Заменяем одинарные обратные слеши на двойные в путях Windows
content = content.replace('"C:\Program Files (x86)\1cv8\common\1cestart.exe"', '"C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe"')
content = content.replace('"C:\Program Files\1cv8\common\1cestart.exe"', '"C:\\Program Files\\1cv8\\common\\1cestart.exe"')

with open('app/src/main/java/l1c/RunYBase.java', 'w', encoding='utf-8') as f:
    f.write(content)

print('Done')
