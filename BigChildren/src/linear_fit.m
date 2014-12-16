function [slope, crossing]=linear_fit(X, Y)
sumx=sum(X); 
sumy=sum(Y); 
sumxy=sum(X.*Y);
sumx2=sum(X.^2);

m=size(X, 2); 

slope=(m*sumxy-sumx*sumy)/(m*sumx2-sumx^2);
crossing=(sumx2*sumy-sumxy*sumx)/(m*sumx2-sumx^2);
end