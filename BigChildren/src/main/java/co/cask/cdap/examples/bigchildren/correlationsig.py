#!/usr/bin/ipython3
import numpy as np
from scipy.stats.stats import pearsonr
#from pydoc import help
#help(pearsonr)

from pylab import *
import numpy.random as npr

pcstore = []
for i in range(1000):

    x = npr.uniform(0, 1, 20)
    y = npr.uniform(0, 1, 20)
    pcc = pearsonr(x,y)
    print(i, pcc)
    pcstore.append(pcc)


hist(np.array(pcstore)[:,0])
show()
import pdb
pdb.set_trace()
